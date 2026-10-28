package br.com.portalmanager.platform.library.redis.core;

import java.util.List;

/** A single SCAN reply. Cursor "0" finishes traversal; COUNT is a hint, not a hard limit. */
public record RedisKeyPage(String cursor, List<String> keys) {
    public RedisKeyPage {
        keys = List.copyOf(keys);
    }
}
