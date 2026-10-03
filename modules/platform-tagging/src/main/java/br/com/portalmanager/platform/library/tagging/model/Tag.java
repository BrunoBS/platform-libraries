package br.com.portalmanager.platform.library.tagging.model;

public interface Tag {

    TagName getName();

    TagOriginType getOriginType();

    void changeOrigin(TagOriginType originType);
}
