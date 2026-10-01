package br.com.portalmanager.platform.library.messaging.resolver;

import br.com.portalmanager.platform.library.messaging.cache.ApiMessageCache;
import br.com.portalmanager.platform.library.messaging.exception.ApiMessageNotFoundException;
import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import br.com.portalmanager.platform.library.messaging.provider.ApiMessageProvider;
import br.com.portalmanager.platform.library.messaging.repository.ApiMessageRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DefaultApiMessageResolver implements ApiMessageResolver {

    private final ApiMessageRepository repository;
    private final ApiMessageCache cache;
    private final Locale defaultLocale;
    private final ApiMessageProvider provider;
    private final String serviceName;

    public DefaultApiMessageResolver(
            ApiMessageRepository repository,
            ApiMessageCache cache,
            Locale defaultLocale
    ) {
        this(repository, cache, defaultLocale, null, null);
    }

    public DefaultApiMessageResolver(
            ApiMessageRepository repository,
            ApiMessageCache cache,
            Locale defaultLocale,
            ApiMessageProvider provider
    ) {
        this(repository, cache, defaultLocale, provider, null);
    }

    public DefaultApiMessageResolver(
            ApiMessageRepository repository,
            ApiMessageCache cache,
            Locale defaultLocale,
            ApiMessageProvider provider,
            String serviceName
    ) {
        this.repository = repository;
        this.cache = cache;
        this.defaultLocale = defaultLocale;
        this.provider = provider;
        this.serviceName = normalizeServiceName(serviceName);
    }

    @Override
    public ApiMessage resolve(String key, Locale locale) {
        List<Locale> localeCandidates = getLocaleCandidates(locale);
        List<String> keyCandidates = getKeyCandidates(key);

        for (Locale candidateLocale : localeCandidates) {
            for (String candidateKey : keyCandidates) {
                Optional<ApiMessage> externalMessage = tryGetFromCache(candidateKey, candidateLocale)
                        .or(() -> tryGetFromRepositoryAndCache(candidateKey, candidateLocale));
                if (externalMessage.isPresent()) {
                    return externalMessage.get();
                }
            }
        }

        for (Locale candidateLocale : localeCandidates) {
            for (String candidateKey : keyCandidates) {
                Optional<ApiMessage> fallbackMessage = tryGetFromProvider(candidateKey, candidateLocale);
                if (fallbackMessage.isPresent()) {
                    return fallbackMessage.get();
                }
            }
        }

        throw new ApiMessageNotFoundException(key);
    }

    private Optional<ApiMessage> tryGetFromCache(String key, Locale locale) {
        return cache.get(key, locale);
    }

    private Optional<ApiMessage> tryGetFromRepositoryAndCache(String key, Locale locale) {
        return repository.find(key, locale).map(message -> {
            cache.put(message);
            return message;
        });
    }

    private Optional<ApiMessage> tryGetFromProvider(String key, Locale locale) {
        if (provider == null) {
            return Optional.empty();
        }
        return provider.find(key, locale);
    }

    private List<String> getKeyCandidates(String key) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (key == null || key.isBlank()) {
            return new ArrayList<>();
        }

        String normalizedKey = key.trim();
        candidates.add(normalizedKey);

        if (serviceName != null && !normalizedKey.startsWith(serviceName + ".")) {
            candidates.add(serviceName + "." + normalizedKey);
        }

        return new ArrayList<>(candidates);
    }

    private List<Locale> getLocaleCandidates(Locale locale) {
        LinkedHashSet<Locale> uniqueCandidates = new LinkedHashSet<>();
        if (locale != null) {
            uniqueCandidates.add(locale);
            uniqueCandidates.add(Locale.forLanguageTag(locale.getLanguage()));
        }
        uniqueCandidates.add(this.defaultLocale);
        uniqueCandidates.add(Locale.forLanguageTag("pt-BR"));
        uniqueCandidates.remove(Locale.ROOT);
        return new ArrayList<>(uniqueCandidates);
    }

    private String normalizeServiceName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
