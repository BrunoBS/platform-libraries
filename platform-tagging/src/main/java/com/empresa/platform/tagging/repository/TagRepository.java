package com.empresa.platform.tagging.repository;

import com.empresa.platform.tagging.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, String> {

    List<Tag> findByOwnerTypeAndOwnerIdOrderByNameAsc(String ownerType, String ownerId);

    long deleteByOwnerTypeAndOwnerId(String ownerType, String ownerId);
}
