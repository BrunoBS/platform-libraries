package br.com.portalmanager.platform.library.tagging.model;

public interface TagRecord {

    String getName();

    TagOriginType getOriginType();

    void changeOrigin(TagOriginType originType);
}
