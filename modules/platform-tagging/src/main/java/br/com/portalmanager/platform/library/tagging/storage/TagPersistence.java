package br.com.portalmanager.platform.library.tagging.storage;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;

import java.util.Collection;
import java.util.List;

public interface TagPersistence<TAG extends Tag, OWNER, OWNER_ID, OWNER_KEY> {

    OWNER_ID ownerId(OWNER owner);

    OWNER_KEY ownerKey(TAG tag);

    TAG newTag(OWNER owner, TagName name, TagOriginType originType);

    List<TAG> findByOwnerId(OWNER_ID ownerId);

    List<TAG> findByOwnerKeysAndOrigin(Collection<OWNER_KEY> ownerKeys, TagOriginType originType);

    List<OWNER_KEY> findOwnerKeysByTag(TagName name);

    void saveAllTags(Collection<TAG> tags);

    void deleteAllTags(Collection<TAG> tags);

    void deleteByOwnerId(OWNER_ID ownerId);
}
