package br.com.portalmanager.platform.tagging.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

import br.com.portalmanager.platform.tagging.validation.TagValidation;

@Entity
@Table(
        name = "tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tags_owner_name",
                columnNames = {"owner_type", "owner_id", "name"}
        )
)
public class Tag {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "owner_type", length = 50, nullable = false)
    private String ownerType;

    @Column(name = "owner_id", length = 100, nullable = false)
    private String ownerId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", length = 20, nullable = false)
    private TagOriginType originType;

    protected Tag() {
    }

    public Tag(TagOwnerType ownerType, Object ownerId, String name, TagOriginType originType) {
        this(
                UUID.randomUUID().toString(),
                TagValidation.requireOwnerType(ownerType),
                TagValidation.requireOwnerId(ownerId),
                TagValidation.requireName(name),
                TagValidation.requireOrigin(originType)
        );
    }

    Tag(String id, String ownerType, String ownerId, String name, TagOriginType originType) {
        this.id = TagValidation.requireId(id);
        this.ownerType = TagValidation.requireOwnerType(ownerType);
        this.ownerId = TagValidation.requireOwnerId(ownerId);
        this.name = TagValidation.requireName(name);
        this.originType = TagValidation.requireOrigin(originType);
    }

    public void changeOrigin(TagOriginType originType) {
        this.originType = TagValidation.requireOrigin(originType);
    }

    public String getId() { return id; }
    public String getOwnerType() { return ownerType; }
    public String getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public TagOriginType getOriginType() { return originType; }
}
