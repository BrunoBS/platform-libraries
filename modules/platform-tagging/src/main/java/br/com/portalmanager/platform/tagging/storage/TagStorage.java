package br.com.portalmanager.platform.tagging.storage;

import br.com.portalmanager.platform.tagging.model.Tag;
import br.com.portalmanager.platform.tagging.model.TagOriginType;

import java.util.Collection;
import java.util.List;

public interface TagStorage {

    List<Tag> findByOwner(String ownerType, String ownerId);

    List<Tag> findByOwnerAndOrigin(
            String ownerType,
            String ownerId,
            TagOriginType originType
    );

    List<Tag> findByOwnersAndOrigin(
            String ownerType,
            Collection<String> ownerIds,
            TagOriginType originType
    );

    List<String> findOwnerIdsByTag(String ownerType, String normalizedTag);

    List<Tag> saveAll(Collection<Tag> tags);

    void deleteAll(Collection<Tag> tags);

    long deleteByOwner(String ownerType, String ownerId);
}
