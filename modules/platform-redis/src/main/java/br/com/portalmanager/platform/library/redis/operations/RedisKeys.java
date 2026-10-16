package br.com.portalmanager.platform.library.redis.operations;

import java.util.function.Consumer;

/** Namespace-scoped, cursor-based key traversal; never materializes all keys. */
public interface RedisKeys {
    /** Literal prefix, not a glob; empty selects this namespace. Redis may yield duplicates. */
    void scan(String prefix, int count, Consumer<String> consumer);
}
