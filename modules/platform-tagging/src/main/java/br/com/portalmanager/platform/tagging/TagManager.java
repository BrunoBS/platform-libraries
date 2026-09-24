package br.com.portalmanager.platform.tagging;

import br.com.portalmanager.platform.tagging.model.Tag;
import br.com.portalmanager.platform.tagging.model.TagOriginType;
import br.com.portalmanager.platform.tagging.model.TagOwnerType;
import br.com.portalmanager.platform.tagging.storage.TagStorage;
import br.com.portalmanager.platform.tagging.validation.TagValidation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TagManager {

    private final TagStorage storage;

    public TagManager(TagStorage storage) {
        this.storage = storage;
    }

    @Transactional
    public List<Tag> reconcile(
            TagOwnerType ownerType,
            Object ownerId,
            Collection<String> manualTags,
            Collection<String> systemTags) {

        String resolvedOwnerType = TagValidation.TagValidation.requireOwnerType(ownerType);
        String resolvedOwnerId = TagValidation.TagValidation.requireOwnerId(ownerId);
        TagOwnerType normalizedOwnerType = () -> resolvedOwnerType;

        Map<String, TagOriginType> desired = desiredTags(manualTags, systemTags);
        List<Tag> current = storage.findByOwner(
                resolvedOwnerType, resolvedOwnerId);

        List<Tag> obsolete = new ArrayList<>();
        List<Tag> result = new ArrayList<>(desired.size());

        for (Tag tag : current) {
            TagOriginType desiredOrigin = desired.remove(tag.getName());
            if (desiredOrigin == null) {
                obsolete.add(tag);
                continue;
            }
            if (tag.getOriginType() != desiredOrigin) {
                tag.changeOrigin(desiredOrigin);
            }
            result.add(tag);
        }

        if (!obsolete.isEmpty()) {
            storage.deleteAll(obsolete);
        }

        List<Tag> created = desired.entrySet().stream()
                .map(entry -> new Tag(
                        normalizedOwnerType,
                        resolvedOwnerId,
                        entry.getKey(),
                        entry.getValue()))
                .toList();

        if (!created.isEmpty()) {
            storage.saveAll(created);
            result.addAll(created);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<Tag> findAll(TagOwnerType ownerType, Object ownerId) {
        return storage.findByOwner(
                TagValidation.requireOwnerType(ownerType),
                TagValidation.requireOwnerId(ownerId));
    }

    @Transactional(readOnly = true)
    public List<String> findManual(TagOwnerType ownerType, Object ownerId) {
        return findByOrigin(ownerType, ownerId, TagOriginType.MANUAL);
    }

    @Transactional(readOnly = true)
    public List<String> findSystem(TagOwnerType ownerType, Object ownerId) {
        return findByOrigin(ownerType, ownerId, TagOriginType.SYSTEM);
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> findManualByOwners(TagOwnerType ownerType, Collection<?> ownerIds) {
        return findByOwnersAndOrigin(ownerType, ownerIds, TagOriginType.MANUAL);
    }

    @Transactional(readOnly = true)
    public List<String> findOwnerIdsByTag(TagOwnerType ownerType, String tag) {
        String normalizedTag = TagNormalizer.normalize(tag);
        if (normalizedTag == null) {
            return List.of();
        }
        return storage.findOwnerIdsByTag(TagValidation.requireOwnerType(ownerType), normalizedTag);
    }

    @Transactional
    public void deleteAll(TagOwnerType ownerType, Object ownerId) {
        storage.deleteByOwner(TagValidation.requireOwnerType(ownerType), TagValidation.requireOwnerId(ownerId));
    }

    private List<String> findByOrigin(TagOwnerType ownerType, Object ownerId, TagOriginType originType) {
        return storage.findByOwnerAndOrigin(
                        TagValidation.requireOwnerType(ownerType), TagValidation.requireOwnerId(ownerId), originType)
                .stream()
                .map(Tag::getName)
                .toList();
    }

    private Map<String, List<String>> findByOwnersAndOrigin(
            TagOwnerType ownerType, Collection<?> ownerIds, TagOriginType originType) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();
        }

        Set<String> resolvedOwnerIds = ownerIds.stream()
                .map(TagValidation::requireOwnerId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Map<String, List<String>> result = new LinkedHashMap<>();
        resolvedOwnerIds.forEach(ownerId -> result.put(ownerId, new ArrayList<>()));

        storage.findByOwnersAndOrigin(
                        TagValidation.requireOwnerType(ownerType), resolvedOwnerIds, originType)
                .forEach(tag -> result.get(tag.getOwnerId()).add(tag.getName()));

        return result;
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
