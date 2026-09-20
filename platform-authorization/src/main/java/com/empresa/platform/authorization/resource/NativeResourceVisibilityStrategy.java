package com.empresa.platform.authorization.resource;

/**
 * Rewrites protected native SQL so resource visibility is enforced by the database.
 */
public interface NativeResourceVisibilityStrategy {

    String apply(String sql);
}
