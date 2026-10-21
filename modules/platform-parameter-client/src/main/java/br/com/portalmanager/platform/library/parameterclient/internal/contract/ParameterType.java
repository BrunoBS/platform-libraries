package br.com.portalmanager.platform.library.parameterclient.internal.contract;

/** Types supported by the producer's published envelope. */
public enum ParameterType {
    CONFIGURATION,
    EXPRESSION;

    public static boolean isSupported(String value) {
        for (ParameterType type : values()) {
            if (type.name().equals(value)) return true;
        }
        return false;
    }
}
