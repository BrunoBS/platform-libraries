package br.com.portalmanager.platform.library.tagging.storage;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

@NoRepositoryBean
public interface TagRepository<TAG extends Tag<OWNER>, OWNER extends TagOwner>
        extends JpaRepository<TAG, Long> {

    @Query("""
            select t
              from #{#entityName} t
             where t.owner.id = :ownerId
             order by t.name
            """)
    List<TAG> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
            select t
              from #{#entityName} t
             where t.owner.identifier in :identifiers
               and t.originType = :originType
             order by t.owner.identifier, t.name
            """)
    List<TAG> findByOwnerIdentifiersAndOrigin(
            @Param("identifiers") Collection<String> identifiers,
            @Param("originType") TagOriginType originType
    );

    @Query("""
            select distinct t.owner.identifier
              from #{#entityName} t
             where t.name = :name
             order by t.owner.identifier
            """)
    List<String> findOwnerIdentifiersByTag(@Param("name") TagName name);

    @Modifying(flushAutomatically = true)
    @Query("delete from #{#entityName} t where t.owner.id = :ownerId")
    int deleteByOwnerId(@Param("ownerId") Long ownerId);
}
