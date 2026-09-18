package com.empresa.platform.tagging.repository;

import com.empresa.platform.tagging.model.Tag;
import com.empresa.platform.tagging.model.TagOriginType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, String> {

    List<Tag> findByOwnerTypeAndOwnerIdOrderByNameAsc(String ownerType, String ownerId);

    List<Tag> findByOwnerTypeAndOwnerIdAndOriginTypeOrderByNameAsc(
            String ownerType, String ownerId, TagOriginType originType);

    List<Tag> findByOwnerTypeAndOwnerIdInAndOriginTypeOrderByOwnerIdAscNameAsc(
            String ownerType, Collection<String> ownerIds, TagOriginType originType);

    long deleteByOwnerTypeAndOwnerId(String ownerType, String ownerId);
}
