package br.com.portalmanager.platform.library.parameterclient;

/** Evaluates published V1 EXPRESSION parameters by their complete logical key. */
public interface ExpressionProvider {
    /** Absence and invalid documents are controlled errors. */
    boolean matches(String key, Object input);

    /** Fall back on controlled read/evaluation failures, logging only the safe error key. */
    boolean matches(String key, Object input, boolean defaultValue);
}
