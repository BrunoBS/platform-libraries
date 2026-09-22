package br.com.portalmanager.platform.tagging.storage;

import br.com.portalmanager.platform.tagging.model.Tag;
import br.com.portalmanager.platform.tagging.model.TagOriginType;
import br.com.portalmanager.platform.tagging.repository.TagRepository;

import java.util.Collection;
import java.util.List;

public class JpaTagStorage implements TagStorage {

    private final TagRepository repository;

    public JpaTagStorage(TagRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Tag> findByOwner(String ownerType, String ownerId) {
        return repository.findByOwnerTypeAndOwnerIdOrderByNameAsc(ownerType, ownerId);
    }

    @Override
    public List<Tag> findByOwnerAndOrigin(
            String ownerType,
            String ownerId,
            TagOriginType originType) {
        return repository.findByOwnerTypeAndOwnerIdAndOriginTypeOrderByNameAsc(
                ownerType,
                ownerId,
                originType
        );
    }

    @Override
    public List<Tag> findByOwnersAndOrigin(
            String ownerType,
            Collection<String> ownerIds,
            TagOriginType originType) {
        return repository.findByOwnerTypeAndOwnerIdInAndOriginTypeOrderByOwnerIdAscNameAsc(
                ownerType,
                ownerIds,
                originType
        );
    }

    @Override
    public List<String> findOwnerIdsByTag(String ownerType, String normalizedTag) {
        return repository.findOwnerIdsByTag(ownerType, normalizedTag);
    }

    @Override
    public List<Tag> saveAll(Collection<Tag> tags) {
        return repository.saveAll(tags);
    }

    @Override
    public void deleteAll(Collection<Tag> tags) {
        repository.deleteAll(tags);
    }

    @Override
    public long deleteByOwner(String ownerType, String ownerId) {
        return repository.deleteByOwnerTypeAndOwnerId(ownerType, ownerId);
    }
}
