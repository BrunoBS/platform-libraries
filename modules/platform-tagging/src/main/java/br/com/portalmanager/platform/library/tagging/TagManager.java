package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.storage.TagPersistence;

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

public final class TagManager<TAG extends Tag, OWNER, OWNER_ID, OWNER_KEY> {

    private final TagPersistence<TAG, OWNER, OWNER_ID, OWNER_KEY> persistence;

    public TagManager(TagPersistence<TAG, OWNER, OWNER_ID, OWNER_KEY> persistence) {
        this.persistence = Objects.requireNonNull(persistence, "tag persistence must not be null");
    }

    public void reconcile(OWNER owner, Collection<String> manualTags, Collection<String> systemTags) {
        OWNER_ID ownerId = requireOwnerId(owner);
        Map<TagName, TagOriginType> desired = desiredTags(manualTags, systemTags);
        List<TAG> current = persistence.findByOwnerId(ownerId);
        validateCurrent(current);

        List<TAG> obsolete = new ArrayList<>();
        for (TAG tag : current) {
            TagName name = requireTagName(tag);
            TagOriginType desiredOrigin = desired.remove(name);
            if (desiredOrigin == null) {
                obsolete.add(tag);
            } else if (tag.getOriginType() != desiredOrigin) {
                tag.changeOrigin(desiredOrigin);
            }
        }

        if (!obsolete.isEmpty()) {
            persistence.deleteAllTags(obsolete);
        }

        List<TAG> created = desired.entrySet().stream()
                .map(entry -> persistence.newTag(owner, entry.getKey(), entry.getValue()))
                .toList();

        if (!created.isEmpty()) {
            persistence.saveAllTags(created);
        }
    }

    public List<String> findManual(OWNER owner) {
        return persistence.findByOwnerId(requireOwnerId(owner)).stream()
                .filter(tag -> tag.getOriginType() == TagOriginType.MANUAL)
                .map(this::requireTagName)
                .map(TagName::value)
                .sorted()
                .toList();
    }

    public Map<OWNER_KEY, List<String>> findManualByOwnerKeys(Collection<OWNER_KEY> ownerKeys) {
        if (ownerKeys == null || ownerKeys.isEmpty()) {
            return Map.of();
        }

        LinkedHashSet<OWNER_KEY> requested = new LinkedHashSet<>(ownerKeys);
        Map<OWNER_KEY, List<String>> result = new LinkedHashMap<>();
        requested.forEach(key -> result.put(key, new ArrayList<>()));

        persistence.findByOwnerKeysAndOrigin(requested, TagOriginType.MANUAL).forEach(tag -> {
            OWNER_KEY key = persistence.ownerKey(tag);
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

    public List<OWNER_KEY> findOwnerKeysByTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return List.of();
        }
        return persistence.findOwnerKeysByTag(TagName.of(tag)).stream().distinct().toList();
    }

    public void deleteAll(OWNER owner) {
        persistence.deleteByOwnerId(requireOwnerId(owner));
    }

    private OWNER_ID requireOwnerId(OWNER owner) {
        return Objects.requireNonNull(persistence.ownerId(owner), "tag owner id must not be null");
    }

    private void validateCurrent(Collection<TAG> tags) {
        Set<TagName> names = new HashSet<>();
        for (TAG tag : tags) {
            TagName name = requireTagName(tag);
            if (!names.add(name)) {
                throw new IllegalStateException("duplicate persisted tag for owner: " + name.value());
            }
        }
    }

    private TagName requireTagName(TAG tag) {
        Objects.requireNonNull(tag, "persisted tag must not be null");
        return Objects.requireNonNull(tag.getName(), "persisted tag name must not be null");
    }

    private static Map<TagName, TagOriginType> desiredTags(
            Collection<String> manualTags,
            Collection<String> systemTags) {

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
