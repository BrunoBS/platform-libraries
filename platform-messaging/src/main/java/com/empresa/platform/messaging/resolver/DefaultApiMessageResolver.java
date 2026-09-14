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
    private final List<ApiMessageProvider> providers;

    public DefaultApiMessageResolver(ApiMessageRepository repository, ApiMessageCache cache, Locale defaultLocale) {
        this(repository, cache, defaultLocale, List.of());
    }

    public DefaultApiMessageResolver(
            ApiMessageRepository repository,
            ApiMessageCache cache,
            Locale defaultLocale,
            List<ApiMessageProvider> providers
    ) {
        this.repository = repository;
        this.cache = cache;
        this.defaultLocale = defaultLocale;
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    @Override
    public ApiMessage resolve(String key, Locale locale) {
        for (Locale candidate : getCandidates(locale)) {
            Optional<ApiMessage> messageOpt = tryGetFromCache(key, candidate)
                    .or(() -> tryGetFromRepositoryAndCache(key, candidate))
                    .or(() -> tryGetFromProviders(key, candidate));
            if (messageOpt.isPresent()) {
                return messageOpt.get();
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

    private Optional<ApiMessage> tryGetFromProviders(String key, Locale locale) {
        for (ApiMessageProvider provider : providers) {
            try {
                Optional<ApiMessage> message = provider.find(key, locale);
                if (message.isPresent()) {
                    return message;
                }
            } catch (Exception ignored) {
                // Um provider de fallback não pode derrubar o fluxo de resolução.
            }
        }
        return Optional.empty();
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
