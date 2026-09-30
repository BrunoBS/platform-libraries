package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagRecord;
import br.com.portalmanager.platform.library.tagging.storage.TagPersistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class TagManager<T extends TagRecord, O, ID, KEY> {

    private final TagPersistence<T, O, ID, KEY> persistence;

    public TagManager(TagPersistence<T, O, ID, KEY> persistence) {
        this.persistence = Objects.requireNonNull(persistence, "tag persistence must not be null");
    }

    public void reconcile(O owner, Collection<String> manualTags, Collection<String> systemTags) {
        ID ownerId = Objects.requireNonNull(persistence.ownerId(owner), "tag owner id must not be null");
        Map<String, TagOriginType> desired = desiredTags(manualTags, systemTags);
        List<T> current = persistence.findByOwnerId(ownerId);
        List<T> obsolete = new ArrayList<>();

        for (T tag : current) {
            TagOriginType desiredOrigin = desired.remove(tag.getName());
            if (desiredOrigin == null) {
                obsolete.add(tag);
            } else if (tag.getOriginType() != desiredOrigin) {
                tag.changeOrigin(desiredOrigin);
            }
        }

        if (!obsolete.isEmpty()) {
            persistence.deleteAllTags(obsolete);
        }

        List<T> created = desired.entrySet().stream()
                .map(entry -> persistence.newTag(owner, entry.getKey(), entry.getValue()))
                .toList();

        if (!created.isEmpty()) {
            persistence.saveAllTags(created);
        }
    }

    public List<String> findManual(O owner) {
        ID ownerId = Objects.requireNonNull(persistence.ownerId(owner), "tag owner id must not be null");
        return persistence.findByOwnerId(ownerId).stream()
                .filter(tag -> tag.getOriginType() == TagOriginType.MANUAL)
                .map(TagRecord::getName)
                .toList();
    }

    public Map<KEY, List<String>> findManualByOwnerKeys(Collection<KEY> ownerKeys) {
        if (ownerKeys == null || ownerKeys.isEmpty()) {
            return Map.of();
        }

        Map<KEY, List<String>> result = new LinkedHashMap<>();
        ownerKeys.forEach(key -> result.putIfAbsent(key, new ArrayList<>()));

        persistence.findByOwnerKeysAndOrigin(result.keySet(), TagOriginType.MANUAL)
                .forEach(tag -> result.computeIfAbsent(persistence.ownerKey(tag), ignored -> new ArrayList<>())
                        .add(tag.getName()));

        return result.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    public List<KEY> findOwnerKeysByTag(String tag) {
        String normalized = TagNormalizer.normalize(tag);
        return normalized == null ? List.of() : persistence.findOwnerKeysByTag(normalized);
    }

    public void deleteAll(O owner) {
        ID ownerId = Objects.requireNonNull(persistence.ownerId(owner), "tag owner id must not be null");
        persistence.deleteByOwnerId(ownerId);
    }

    private static Map<String, TagOriginType> desiredTags(
            Collection<String> manualTags,
            Collection<String> systemTags) {

        Map<String, TagOriginType> desired = new LinkedHashMap<>();
        normalize(manualTags).forEach(name -> desired.put(name, TagOriginType.MANUAL));
        normalize(systemTags).forEach(name -> desired.putIfAbsent(name, TagOriginType.SYSTEM));
        return desired;
    }

    private static Set<String> normalize(Collection<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        values.stream()
                .map(TagNormalizer::normalize)
                .filter(Objects::nonNull)
                .forEach(normalized::add);
        return normalized;
    }
}
