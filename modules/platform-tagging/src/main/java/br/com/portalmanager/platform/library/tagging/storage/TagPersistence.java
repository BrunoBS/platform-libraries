package br.com.portalmanager.platform.library.tagging.storage;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagRecord;

import java.util.Collection;
import java.util.List;

public interface TagPersistence<T extends TagRecord, O, ID, KEY> {

    ID ownerId(O owner);

    KEY ownerKey(T tag);

    T newTag(O owner, String name, TagOriginType originType);

    List<T> findByOwnerId(ID ownerId);

    List<T> findByOwnerKeysAndOrigin(Collection<KEY> ownerKeys, TagOriginType originType);

    List<KEY> findOwnerKeysByTag(String normalizedTag);

    void saveAllTags(Collection<T> tags);

    void deleteAllTags(Collection<T> tags);

    void deleteByOwnerId(ID ownerId);
}
