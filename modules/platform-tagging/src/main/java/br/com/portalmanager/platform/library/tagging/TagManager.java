package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;
import br.com.portalmanager.platform.library.tagging.storage.TagRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class TagManager<TAG extends Tag<OWNER>, OWNER extends TagOwner> {

    private final TagRepository<TAG, OWNER> repository;
    private final TagFactory<TAG, OWNER> factory;

    public TagManager(TagRepository<TAG, OWNER> repository, TagFactory<TAG, OWNER> factory) {
        this.repository = Objects.requireNonNull(repository, "tag repository must not be null");
        this.factory = Objects.requireNonNull(factory, "tag factory must not be null");
    }

    public void reconcile(OWNER owner, Collection<String> manualTags, Collection<String> systemTags) {
        requireOwner(owner);
        Map<TagName, TagOriginType> desired = desiredTags(manualTags, systemTags);
        List<TAG> current = repository.findByOwnerId(owner.getId());
        validateCurrent(current);

        List<TAG> obsolete = new ArrayList<>();
        List<TAG> changed = new ArrayList<>();

        for (TAG tag : current) {
            TagName name = requireTagName(tag);
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

        List<TAG> created = desired.entrySet().stream()
                .map(entry -> createTag(owner, entry.getKey(), entry.getValue()))
                .toList();

        if (!changed.isEmpty() || !created.isEmpty()) {
            List<TAG> toSave = new ArrayList<>(changed.size() + created.size());
            toSave.addAll(changed);
            toSave.addAll(created);
            repository.saveAll(toSave);
        }
    }

    public List<String> findManual(OWNER owner) {
        requireOwner(owner);
        return repository.findByOwnerId(owner.getId()).stream()
                .filter(tag -> tag.getOriginType() == TagOriginType.MANUAL)
                .map(this::requireTagName)
                .map(TagName::value)
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

        repository.findByOwnerIdentifiersAndOrigin(requested, TagOriginType.MANUAL).forEach(tag -> {
            String key = requireOwner(tag.getOwner()).getIdentifier();
            List<String> tags = result.get(key);
            if (tags != null) {
                tags.add(requireTagName(tag).value());
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
        return repository.findOwnerIdentifiersByTag(TagName.of(tag)).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public void deleteAll(OWNER owner) {
        requireOwner(owner);
        repository.deleteByOwnerId(owner.getId());
    }

    private OWNER requireOwner(OWNER owner) {
        Objects.requireNonNull(owner, "tag owner must not be null");
        Objects.requireNonNull(owner.getId(), "tag owner id must not be null");
        String identifier = Objects.requireNonNull(
                owner.getIdentifier(), "tag owner identifier must not be null");
        if (identifier.isBlank()) {
            throw new IllegalArgumentException("tag owner identifier must not be blank");
        }
        return owner;
    }

    private TAG createTag(OWNER owner, TagName name, TagOriginType originType) {
        TAG tag = Objects.requireNonNull(
                factory.create(owner, name, originType),
                "tag factory must not return null");

        OWNER tagOwner = requireOwner(tag.getOwner());
        if (!Objects.equals(owner.getId(), tagOwner.getId())
                || !Objects.equals(owner.getIdentifier(), tagOwner.getIdentifier())) {
            throw new IllegalStateException("tag factory returned a tag for a different owner");
        }
        if (!name.equals(requireTagName(tag))) {
            throw new IllegalStateException("tag factory returned a tag with a different name");
        }
        if (tag.getOriginType() != originType) {
            throw new IllegalStateException("tag factory returned a tag with a different origin");
        }
        return tag;
    }

    private void validateCurrent(Collection<TAG> tags) {
        Objects.requireNonNull(tags, "persisted tags must not be null");
        Set<TagName> names = new HashSet<>();
        for (TAG tag : tags) {
            TagName name = requireTagName(tag);
            Objects.requireNonNull(tag.getOriginType(), "persisted tag origin must not be null");
            if (!names.add(name)) {
                throw new IllegalStateException("duplicate persisted tag for owner: " + name.value());
            }
        }
    }

    private TagName requireTagName(TAG tag) {
        Objects.requireNonNull(tag, "persisted tag must not be null");
        return Objects.requireNonNull(tag.getName(), "persisted tag name must not be null");
    }

    private static Map<TagName, TagOriginType> desiredTags(Collection<String> manualTags, Collection<String> systemTags) {
        Map<TagName, TagOriginType> desired = new LinkedHashMap<>();
        tagNames(manualTags).forEach(name -> desired.put(name, TagOriginType.MANUAL));
        tagNames(systemTags).forEach(name -> desired.putIfAbsent(name, TagOriginType.SYSTEM));
        return desired;
    }

    private static Set<TagName> tagNames(Collection<String> values) {
        if (values == null) {
            return Set.of();
        }
        Set<TagName> names = new LinkedHashSet<>();
        values.stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .map(TagName::of)
                .forEach(names::add);
        return names;
    }
}
