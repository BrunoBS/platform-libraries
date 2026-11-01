package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.internal.TagNameNormalizer;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;
import br.com.portalmanager.platform.library.tagging.exception.TaggingTechnicalErrors;
import br.com.portalmanager.platform.library.tagging.message.TaggingMessageKeys;
import br.com.portalmanager.platform.library.tagging.storage.TagOwnerTagName;
import br.com.portalmanager.platform.library.tagging.storage.TagRepository;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class TagManager<TAG extends Tag<OWNER>, OWNER extends TagOwner> {

    private final TagRepository<TAG, OWNER> repository;
    private final TagFactory<TAG, OWNER> factory;

    public TagManager(TagRepository<TAG, OWNER> repository, TagFactory<TAG, OWNER> factory) {
        if (repository == null || factory == null) {
            throw new PlatformConfigurationException(TaggingTechnicalErrors.MANAGER_DEPENDENCIES_REQUIRED);
        }
        this.repository = repository;
        this.factory = factory;
    }

    /**
     * Inserts tags for a newly persisted owner that has no existing tags.
     * The caller must use the same transaction as owner creation.
     */
    public List<String> createInitialTags(OWNER owner, Collection<String> manualTags) {
        requireOwner(owner);
        Map<String, TagOriginType> desired = desiredTags(owner, manualTags);
        List<TAG> created = createTags(owner, desired);
        if (!created.isEmpty()) {
            repository.saveAll(created);
        }
        return manualTagNames(desired);
    }

    public void reconcile(OWNER owner, Collection<String> manualTags) {
        reconcileAndReturnManualTags(owner, manualTags);
    }

    /** Returns normalized, sorted manual tags without querying them again. */
    public List<String> reconcileAndReturnManualTags(OWNER owner, Collection<String> manualTags) {
        requireOwner(owner);
        Map<String, TagOriginType> desired = desiredTags(owner, manualTags);
        List<String> normalizedManualTags = manualTagNames(desired);
        List<TAG> current = repository.findByOwnerId(owner.getId());
        validateCurrent(current);

        List<TAG> obsolete = new ArrayList<>();
        List<TAG> changed = new ArrayList<>();

        for (TAG tag : current) {
            String name = requireTagName(tag);
            TagOriginType desiredOrigin = desired.remove(name);
            if (desiredOrigin == null) {
                obsolete.add(tag);
            } else if (tag.getOriginType() != desiredOrigin) {
                tag.changeOrigin(desiredOrigin);
                changed.add(tag);
            }
        }

        if (!obsolete.isEmpty()) {
            repository.deleteAll(obsolete);
        }

        List<TAG> created = createTags(owner, desired);

        if (!changed.isEmpty() || !created.isEmpty()) {
            List<TAG> toSave = new ArrayList<>(changed.size() + created.size());
            toSave.addAll(changed);
            toSave.addAll(created);
            repository.saveAll(toSave);
        }
        return normalizedManualTags;
    }

    private List<TAG> createTags(OWNER owner, Map<String, TagOriginType> desired) {
        return desired.entrySet().stream()
                .map(entry -> {
                    TAG tag = factory.create(owner, entry.getKey(), entry.getValue());
                    if (tag == null) {
                        throw new ApiException(TaggingTechnicalErrors.FACTORY_RESULT_INVALID);
                    }
                    return tag;
                })
                .toList();
    }

    private static List<String> manualTagNames(Map<String, TagOriginType> desired) {
        return desired.entrySet().stream()
                .filter(entry -> entry.getValue() == TagOriginType.MANUAL)
                .map(entry -> entry.getKey())
                .sorted()
                .toList();
    }

    public List<String> findManual(OWNER owner) {
        requireOwner(owner);
        List<TAG> current = repository.findByOwnerId(owner.getId());
        validateCurrent(current);
        return current.stream()
                .filter(tag -> tag.getOriginType() == TagOriginType.MANUAL)
                .map(this::requireTagName)
                .sorted()
                .toList();
    }

    public Map<String, List<String>> findManualByOwnerKeys(Collection<String> ownerKeys) {
        if (ownerKeys == null || ownerKeys.isEmpty()) {
            return Map.of();
        }

        LinkedHashSet<String> requested = ownerKeys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requested.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        requested.forEach(key -> result.put(key, new ArrayList<>()));

        List<TagOwnerTagName> persisted = repository.findOwnerTagNamesByIdentifiersAndOrigin(
                requested, TagOriginType.MANUAL);
        if (persisted == null) {
            throw new ApiException(TaggingTechnicalErrors.PERSISTED_STATE_INVALID);
        }
        persisted.forEach(tag -> {
            if (tag == null || tag.ownerIdentifier() == null || tag.tagName() == null) {
                throw new ApiException(TaggingTechnicalErrors.PERSISTED_STATE_INVALID);
            }
            List<String> tags = result.get(tag.ownerIdentifier());
            if (tags != null) {
                tags.add(tag.tagName());
            }
        });

        return result.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().distinct().sorted().toList(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    public List<String> findOwnerKeysByTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return List.of();
        }
        return TagNameNormalizer.normalizeIfPresent(tag)
                .map(name -> repository.findOwnerIdentifiersByTag(name).stream()
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .orElseGet(List::of);
    }

    public void deleteAll(OWNER owner) {
        requireOwner(owner);
        repository.deleteByOwnerId(owner.getId());
    }

    private OWNER requireOwner(OWNER owner) {
        if (owner == null || owner.getId() == null
                || owner.getIdentifier() == null || owner.getIdentifier().isBlank()) {
            throw new ValidationException(
                    PlatformMessageKeys.VALIDATION_FAILED,
                    List.of(new ValidationDetail("owner", TaggingMessageKeys.OWNER_INVALID))
            );
        }
        return owner;
    }

    private void validateCurrent(Collection<TAG> tags) {
        if (tags == null) {
            throw new ApiException(TaggingTechnicalErrors.PERSISTED_STATE_INVALID);
        }
        Set<String> names = new HashSet<>();
        for (TAG tag : tags) {
            String name = requireTagName(tag);
            if (tag.getOriginType() == null) {
                throw new ApiException(TaggingTechnicalErrors.PERSISTED_STATE_INVALID);
            }
            if (!names.add(name)) {
                throw new ConflictException(PlatformMessageKeys.DATA_INTEGRITY);
            }
        }
    }

    private String requireTagName(TAG tag) {
        if (tag == null || tag.getName() == null) {
            throw new ApiException(TaggingTechnicalErrors.PERSISTED_STATE_INVALID);
        }
        try {
            return TagNameNormalizer.normalizeRequired(tag.getName());
        } catch (ValidationException exception) {
            throw new ApiException(TaggingTechnicalErrors.PERSISTED_NAME_INVALID, exception);
        }
    }

    private Map<String, TagOriginType> desiredTags(OWNER owner, Collection<String> manualTags) {
        Map<String, TagOriginType> desired = new LinkedHashMap<>();
        tagNames(manualTags).forEach(name -> desired.put(name, TagOriginType.MANUAL));
        Collection<String> systemTags = owner.getSystemTags();
        systemTags = systemTags == null ? List.of() : systemTags;
        tagNames(systemTags).forEach(name -> desired.putIfAbsent(name, TagOriginType.SYSTEM));
        return desired;
    }

    private static Set<String> tagNames(Collection<String> values) {
        if (values == null) {
            return Set.of();
        }
        Set<String> names = new LinkedHashSet<>();
        values.stream()
                .map(TagNameNormalizer::normalizeIfPresent)
                .flatMap(java.util.Optional::stream)
                .forEach(names::add);
        return names;
    }
}
