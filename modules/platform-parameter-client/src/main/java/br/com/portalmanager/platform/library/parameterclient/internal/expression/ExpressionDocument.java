package br.com.portalmanager.platform.library.parameterclient.internal.expression;

import br.com.portalmanager.platform.library.parameterclient.internal.contract.ExpressionOperator;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ExpressionValueType;
import tools.jackson.databind.JsonNode;

/** Expression structure already checked by ExpressionDocumentReader. */
record ExpressionDocument(ExpressionOperator operator, ExpressionValueType valueType, JsonNode value) {}
