package br.com.portalmanager.platform.library.tagging.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.Objects;

@MappedSuperclass
public abstract class Tag<OWNER extends TagOwner> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = TagNameConverter.class)
    @Column(name = "name", nullable = false, length = TagName.MAX_LENGTH)
    private TagName name;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 20)
    private TagOriginType originType;

    protected Tag() {
    }

    protected Tag(TagName name, TagOriginType originType) {
        this.name = Objects.requireNonNull(name, "tag name must not be null");
        this.originType = Objects.requireNonNull(originType, "tag origin must not be null");
    }

    public Long getId() {
        return id;
    }

    public TagName getName() {
        return name;
    }

    public TagOriginType getOriginType() {
        return originType;
    }

    public void changeOrigin(TagOriginType originType) {
        this.originType = Objects.requireNonNull(originType, "tag origin must not be null");
    }

    public abstract OWNER getOwner();
}
