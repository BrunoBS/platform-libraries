package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagRecord;
import br.com.portalmanager.platform.library.tagging.storage.TagPersistence;

import java.util.*;
import java.util.stream.Collectors;

public final class TagManager<T extends TagRecord, O, ID, KEY> {

    private final TagPersistence<T, O, ID, KEY> persistence;

    public TagManager(TagPersistence<T, O, ID, KEY> persistence) {
        this.persistence = Objects.requireNonNull(persistence, "tag persistence must not be null");
    }

    public void reconcile(O owner, Collection<String> manualTags, Collection<String> systemTags) {
        ID ownerId = requireOwnerId(owner);
        Map<TagName, TagOriginType> desired = desiredTags(manualTags, systemTags);
        List<T> current = persistence.findByOwnerId(ownerId);
        validateCurrent(current);

        List<T> obsolete = new ArrayList<>();
        for (T tag : current) {
            TagName name = persistedName(tag);
            TagOriginType desiredOrigin = desired.remove(name);
            if (desiredOrigin == null) {
                obsolete.add(tag);
            } else if (tag.getOriginType() != desiredOrigin) {
                tag.changeOrigin(desiredOrigin);
            }
        }

        if (!obsolete.isEmpty()) persistence.deleteAllTags(obsolete);

        List<T> created = desired.entrySet().stream()
                .map(entry -> persistence.newTag(owner, entry.getKey().value(), entry.getValue()))
                .toList();
        if (!created.isEmpty()) persistence.saveAllTags(created);
    }

    public List<String> findManual(O owner) {
        return persistence.findByOwnerId(requireOwnerId(owner)).stream()
                .filter(tag -> tag.getOriginType() == TagOriginType.MANUAL)
                .map(this::persistedName)
                .map(TagName::value)
                .sorted()
                .toList();
    }

    public Map<KEY, List<String>> findManualByOwnerKeys(Collection<KEY> ownerKeys) {
        if (ownerKeys == null || ownerKeys.isEmpty()) return Map.of();

        LinkedHashSet<KEY> requested = new LinkedHashSet<>(ownerKeys);
        Map<KEY, List<String>> result = new LinkedHashMap<>();
        requested.forEach(key -> result.put(key, new ArrayList<>()));

        persistence.findByOwnerKeysAndOrigin(requested, TagOriginType.MANUAL).forEach(tag -> {
            KEY key = persistence.ownerKey(tag);
            List<String> tags = result.get(key);
            if (tags != null) tags.add(persistedName(tag).value());
        });

        return result.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().distinct().sorted().toList(),
                (left, right) -> left,
                LinkedHashMap::new));
    }

    public List<KEY> findOwnerKeysByTag(String tag) {
        String normalized = TagNormalizer.normalize(tag);
        return normalized == null ? List.of() : persistence.findOwnerKeysByTag(normalized).stream().distinct().toList();
    }

    public void deleteAll(O owner) {
        persistence.deleteByOwnerId(requireOwnerId(owner));
    }

    private ID requireOwnerId(O owner) {
        return Objects.requireNonNull(persistence.ownerId(owner), "tag owner id must not be null");
    }

    private void validateCurrent(Collection<T> tags) {
        Set<TagName> names = new HashSet<>();
        for (T tag : tags) {
            TagName name = persistedName(tag);
            if (!names.add(name)) {
                throw new IllegalStateException("duplicate persisted tag for owner: " + name.value());
            }
        }
    }

    private TagName persistedName(T tag) {
        Objects.requireNonNull(tag, "persisted tag must not be null");
        TagName name = TagName.of(tag.getName());
        if (!name.value().equals(tag.getName())) {
            throw new IllegalStateException("persisted tag name must be normalized: " + tag.getName());
        }
        return name;
    }

    private static Map<TagName, TagOriginType> desiredTags(Collection<String> manualTags, Collection<String> systemTags) {
        Map<TagName, TagOriginType> desired = new LinkedHashMap<>();
        normalize(manualTags).forEach(name -> desired.put(name, TagOriginType.MANUAL));
        normalize(systemTags).forEach(name -> desired.putIfAbsent(name, TagOriginType.SYSTEM));
        return desired;
    }

    private static Set<TagName> normalize(Collection<String> values) {
        if (values == null) return Set.of();
        Set<TagName> normalized = new LinkedHashSet<>();
        values.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).map(TagName::of).forEach(normalized::add);
        return normalized;
    }
}
