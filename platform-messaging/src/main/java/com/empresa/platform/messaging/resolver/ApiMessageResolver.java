package com.empresa.platform.messaging.resolver;

import com.empresa.platform.messaging.model.ApiMessage;

import java.util.Locale;

public interface ApiMessageResolver {
    ApiMessage resolve(String messageKey, Locale locale);
}
