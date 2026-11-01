package br.com.portalmanager.platform.library.tagging.model;

import jakarta.persistence.Column;
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

    @Column(name = "name", nullable = false, length = TagName.MAX_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 20)
    private TagOriginType originType;

    protected Tag() {
    }

    protected Tag(String name, TagOriginType originType) {
        this.name = TagName.of(name).value();
        if (originType == null) {
            throw validation("origin", TaggingMessageKeys.TAG_ORIGIN_REQUIRED);
        }
        this.originType = originType;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
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
