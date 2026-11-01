package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;

@FunctionalInterface
public interface TagFactory<TAG extends Tag<OWNER>, OWNER extends TagOwner> {

    TAG create(OWNER owner, String name, TagOriginType originType);
}
