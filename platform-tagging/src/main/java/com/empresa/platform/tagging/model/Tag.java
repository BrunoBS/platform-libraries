package com.empresa.platform.tagging.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.UUID;

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
        this(UUID.randomUUID().toString(), ownerType.value(), String.valueOf(ownerId), name, originType);
    }

    Tag(String id, String ownerType, String ownerId, String name, TagOriginType originType) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.name = Objects.requireNonNull(name, "name");
        this.originType = Objects.requireNonNull(originType, "originType");
    }

    public String getId() { return id; }
    public String getOwnerType() { return ownerType; }
    public String getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public TagOriginType getOriginType() { return originType; }
}
