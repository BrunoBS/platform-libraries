package br.com.portalmanager.platform.library.tagging.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import br.com.portalmanager.platform.library.tagging.message.TaggingMessageKeys;

import java.util.List;

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
        if (name == null) {
            throw validation("tag", TaggingMessageKeys.TAG_NAME_INVALID);
        }
        if (originType == null) {
            throw validation("origin", TaggingMessageKeys.TAG_ORIGIN_REQUIRED);
        }
        this.name = name;
        this.originType = originType;
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
        if (originType == null) {
            throw validation("origin", TaggingMessageKeys.TAG_ORIGIN_REQUIRED);
        }
        this.originType = originType;
    }

    private static ValidationException validation(String field, String messageKey) {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(new ValidationDetail(field, messageKey))
        );
    }

    public abstract OWNER getOwner();
}
