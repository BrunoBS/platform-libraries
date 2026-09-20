package com.empresa.platform.authorization.resource;

/**
 * Thread-bound state used by native resource-visibility rewriting.
 *
 * <p>The scope is reference-counted so nested {@code @ResourceVisibility}
 * invocations cannot clear the outer state. OWNER is retained in the context
 * because native queries need to preserve the visibility parameter while
 * bypassing only its mandatory predicate.</p>
 */
public class NativeResourceVisibilityContext {

    private final ThreadLocal<State> state = new ThreadLocal<>();

    public void enter(boolean owner) {
        State current = state.get();
        if (current == null) {
            state.set(new State(1, owner));
            return;
        }
        state.set(new State(current.depth() + 1, current.owner() || owner));
    }

    public void enter() {
        enter(false);
    }

    public void exit() {
        State current = state.get();
        if (current == null || current.depth() <= 1) {
            state.remove();
            return;
        }
        state.set(new State(current.depth() - 1, current.owner()));
    }

    public boolean isActive() {
        return state.get() != null;
    }

    public boolean isOwner() {
        State current = state.get();
        return current != null && current.owner();
    }

    private record State(int depth, boolean owner) {
    }
}
