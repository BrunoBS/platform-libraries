package com.empresa.platform.audit.context;

import com.empresa.platform.audit.model.AuditContext;

public interface AuditContextProvider {
    AuditContext currentContext();
}
