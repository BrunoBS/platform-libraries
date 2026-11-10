package br.com.portalmanager.platform.library.tagging.api.contract;

import br.com.portalmanager.platform.library.tagging.api.contract.model.Tag;
import br.com.portalmanager.platform.library.tagging.api.contract.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.api.contract.model.TagOwner;

@FunctionalInterface
public interface TagFactory<TAG extends Tag<OWNER>, OWNER extends TagOwner> {

    TAG create(OWNER owner, String name, TagOriginType originType);
}
