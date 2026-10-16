package br.com.portalmanager.platform.library.redis.operations;

/** Native Redis operations; bounded reads and JSON values, no implicit type conversion. */
public interface RedisAtomic {
    /** Sets a persistent JSON value only while the guard still equals expectedGuard. */
    <T> boolean setIfValue(String guardKey, Object expectedGuard, String key, T value);

    /** Deletes the target while the guard matches; true also when the target is already absent. */
    boolean deleteIfValue(String guardKey, Object expectedGuard, String key);

    /** Atomically deletes a key only if its JSON value equals expected. */
    boolean compareAndDelete(String key, Object expected);

    long increment(String key, long positiveDelta);

    long decrement(String key, long positiveDelta);

    <T> boolean setIfAbsent(String key, T value);

    <T> boolean compareAndSet(String key, T expected, T replacement);
}
