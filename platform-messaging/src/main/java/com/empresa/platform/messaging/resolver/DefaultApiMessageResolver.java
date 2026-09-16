package com.empresa.platform.messaging.resolver;

import com.empresa.platform.messaging.cache.ApiMessageCache;
import com.empresa.platform.messaging.exception.ApiMessageNotFoundException;
import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.provider.ApiMessageProvider;
import com.empresa.platform.messaging.repository.ApiMessageRepository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DefaultApiMessageResolver implements ApiMessageResolver {

    private final ApiMessageRepository repository;
    private final ApiMessageCache cache;
    private final Locale defaultLocale;
    private final ApiMessageProvider provider;

    public DefaultApiMessageResolver(
            ApiMessageRepository repository,
            ApiMessageCache cache,
            Locale defaultLocale,
            ApiMessageProvider provider
    ) {
        this.repository = repository;
        this.cache = cache;
        this.defaultLocale = defaultLocale;
        this.provider = provider;
    }

    @Override
    public ApiMessage resolve(String key, Locale locale) {
        List<Locale> candidates = getCandidates(locale);

        for (Locale candidate : candidates) {
            Optional<ApiMessage> externalMessage = tryGetFromCache(key, candidate)
                    .or(() -> tryGetFromRepositoryAndCache(key, candidate));
            if (externalMessage.isPresent()) {
                return externalMessage.get();
            }
        }

        for (Locale candidate : candidates) {
            Optional<ApiMessage> fallbackMessage = tryGetFromProvider(key, candidate);
            if (fallbackMessage.isPresent()) {
                return fallbackMessage.get();
            }
        }

        throw new ApiMessageNotFoundException(key);
    }

    private Optional<ApiMessage> tryGetFromCache(String key, Locale locale) {
        try {
            return cache.get(key, locale);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<ApiMessage> tryGetFromRepositoryAndCache(String key, Locale locale) {
        return repository.find(key, locale).map(message -> {
            try {
                cache.put(message);
            } catch (Exception ignored) {
            }
            return message;
        });
    }

    private Optional<ApiMessage> tryGetFromProvider(String key, Locale locale) {
        if (provider == null) {
            return Optional.empty();
        }
        try {
            return provider.find(key, locale);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private List<Locale> getCandidates(Locale locale) {
        var uniqueCandidates = new java.util.LinkedHashSet<Locale>();
        if (locale != null) {
            uniqueCandidates.add(locale);
            uniqueCandidates.add(Locale.forLanguageTag(locale.getLanguage()));
        }
        uniqueCandidates.add(this.defaultLocale);
        uniqueCandidates.add(Locale.forLanguageTag("pt-BR"));
        uniqueCandidates.remove(Locale.ROOT);
        return new java.util.ArrayList<>(uniqueCandidates);
    }
}
