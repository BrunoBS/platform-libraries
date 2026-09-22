package br.com.portalmanager.platform.tagging.repository;

import br.com.portalmanager.platform.tagging.model.Tag;
import br.com.portalmanager.platform.tagging.model.TagOriginType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, String> {

    List<Tag> findByOwnerTypeAndOwnerIdOrderByNameAsc(String ownerType, String ownerId);

    List<Tag> findByOwnerTypeAndOwnerIdAndOriginTypeOrderByNameAsc(
            String ownerType, String ownerId, TagOriginType originType);

    List<Tag> findByOwnerTypeAndOwnerIdInAndOriginTypeOrderByOwnerIdAscNameAsc(
            String ownerType, Collection<String> ownerIds, TagOriginType originType);

    @Query("""
            select distinct t.ownerId
              from Tag t
             where t.ownerType = :ownerType
               and lower(t.name) like lower(concat('%', :name, '%'))
             order by t.ownerId
            """)
    List<String> findOwnerIdsByTag(
            @Param("ownerType") String ownerType,
            @Param("name") String name
    );

    long deleteByOwnerTypeAndOwnerId(String ownerType, String ownerId);
}
