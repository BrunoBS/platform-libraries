package com.empresa.platform.authorization.resource;

/**
 * Thread-bound marker used by Spring Data native query rewriting.
 *
 * <p>The context is active only while a non-OWNER {@code @ResourceVisibility}
 * invocation is executing. Nesting is reference-counted so inner annotated
 * calls cannot clear an outer visibility scope.</p>
 */
public class NativeResourceVisibilityContext {

    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    public void enter() {
        depth.set(depth.get() + 1);
    }

    public void exit() {
        int current = depth.get();
        if (current <= 1) {
            depth.remove();
            return;
        }
        depth.set(current - 1);
    }

    public boolean isActive() {
        return depth.get() > 0;
    }
}
