package br.com.portalmanager.platform.library.tagging.model;

import java.util.Collection;

public interface TagOwner {

    Long getId();

    String getIdentifier();

    Collection<String> getSystemTags();
}
