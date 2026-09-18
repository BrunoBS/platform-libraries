package com.empresa.platform.tagging;

import com.empresa.platform.tagging.model.Tag;
import com.empresa.platform.tagging.model.TagOriginType;
import com.empresa.platform.tagging.model.TagOwnerType;
import com.empresa.platform.tagging.repository.TagRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TagManager {

    private final TagRepository repository;

    public TagManager(TagRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<Tag> reconcile(
            TagOwnerType ownerType,
            Object ownerId,
            Collection<String> manualTags,
            Collection<String> systemTags) {

        String resolvedOwnerType = requireOwnerType(ownerType);
        String resolvedOwnerId = requireOwnerId(ownerId);
        TagOwnerType normalizedOwnerType = () -> resolvedOwnerType;

        Map<String, TagOriginType> desired = desiredTags(manualTags, systemTags);
        List<Tag> current = repository.findByOwnerTypeAndOwnerIdOrderByNameAsc(
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
            repository.deleteAll(obsolete);
        }

        List<Tag> created = desired.entrySet().stream()
                .map(entry -> new Tag(
                        normalizedOwnerType,
                        resolvedOwnerId,
                        entry.getKey(),
                        entry.getValue()))
                .toList();

        if (!created.isEmpty()) {
            repository.saveAll(created);
            result.addAll(created);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<Tag> findAll(TagOwnerType ownerType, Object ownerId) {
        return repository.findByOwnerTypeAndOwnerIdOrderByNameAsc(
                requireOwnerType(ownerType),
                requireOwnerId(ownerId));
    }

    @Transactional
    public void deleteAll(TagOwnerType ownerType, Object ownerId) {
        repository.deleteByOwnerTypeAndOwnerId(requireOwnerType(ownerType), requireOwnerId(ownerId));
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

    private static String requireOwnerType(TagOwnerType ownerType) {
        Objects.requireNonNull(ownerType, "ownerType");
        String value = ownerType.value();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ownerType must not be blank");
        }
        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String requireOwnerId(Object ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        String value = String.valueOf(ownerId).trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        return value;
    }
}
