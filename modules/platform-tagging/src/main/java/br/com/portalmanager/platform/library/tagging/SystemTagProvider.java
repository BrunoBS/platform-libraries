package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.TagOwner;

import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface SystemTagProvider<OWNER extends TagOwner> {

    Collection<String> resolve(OWNER owner);

    static <OWNER extends TagOwner> SystemTagProvider<OWNER> none() {
        return owner -> List.of();
    }
}
