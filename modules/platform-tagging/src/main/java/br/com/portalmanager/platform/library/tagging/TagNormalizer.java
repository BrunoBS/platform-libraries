package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.TagName;

public final class TagNormalizer {

    private TagNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return TagName.of(value).value();
    }
}
