package br.com.portalmanager.platform.library.parameterclient.internal.contract;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

/** Published JSON field names and concrete Java types supported by default-based reads. */
public final class ParameterFields {
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String OPERATOR = "operator";
    public static final String VALUE_TYPE = "valueType";

    public static final Set<Class<?>> INFERABLE = Set.of(
            String.class, Boolean.class, Byte.class, Short.class, Integer.class, Long.class,
            Float.class, Double.class, BigInteger.class, BigDecimal.class);

    private ParameterFields() {}
}
