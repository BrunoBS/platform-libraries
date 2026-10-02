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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultApiMessageResolver implements ApiMessageResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultApiMessageResolver.class);
    private static final Duration SOURCE_FAILURE_LOG_INTERVAL = Duration.ofMinutes(10);
    private static final String DEFAULT_CODE = "PLT-500";
    public static final String DEFAULT_KEY = "platform.internal.error";
    private static final String DEFAULT_MESSAGE = "Ocorreu um erro inesperado.";
    private static final String DEFAULT_SOLUTION = "Tente novamente. Se o problema persistir, contate o suporte.";
    private static final ConcurrentMap<String, Long> LAST_FAILURE_LOG = new ConcurrentHashMap<>();

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

        logFailure("definition", key, null, "Platform message definition not found; using immutable default");
        return platformDefault(locale);
    }

    private Optional<ApiMessage> tryGetFromCache(String key, Locale locale) {
        try {
            return cache.get(key, locale);
        } catch (RuntimeException exception) {
            logFailure("cache", key, exception, "Platform messaging cache unavailable; continuing fallback");
            return Optional.empty();
        }
    }

    private Optional<ApiMessage> tryGetFromRepositoryAndCache(String key, Locale locale) {
        try {
            return repository.find(key, locale).map(message -> {
                try {
                    cache.put(message);
                } catch (RuntimeException exception) {
                    logFailure("cache", key, exception, "Platform messaging cache unavailable while storing message; continuing");
                }
                return message;
            });
        } catch (RuntimeException exception) {
            logFailure("datasource", key, exception, "Platform messaging datasource unavailable; continuing fallback");
            return Optional.empty();
        }
    }

    private Optional<ApiMessage> tryGetFromProvider(String key, Locale locale) {
        if (provider == null) {
            return Optional.empty();
        }
        try {
            return provider.find(key, locale);
        } catch (RuntimeException exception) {
            logFailure("bundle", key, exception, "Platform messaging bundle unavailable; using immutable default if necessary");
            return Optional.empty();
        }
    }

    private ApiMessage platformDefault(Locale locale) {
        String languageTag = locale == null || Locale.ROOT.equals(locale) ? defaultLocale.toLanguageTag() : locale.toLanguageTag();
        return new ApiMessage(DEFAULT_CODE, DEFAULT_KEY, languageTag, DEFAULT_MESSAGE, DEFAULT_SOLUTION, 500);
    }

    private void logFailure(String source, String key, Throwable cause, String message) {
        String throttleKey = source + ":" + (cause == null ? "missing" : cause.getClass().getName());
        long now = System.currentTimeMillis();
        Long previous = LAST_FAILURE_LOG.putIfAbsent(throttleKey, now);
        if (previous != null && now - previous < SOURCE_FAILURE_LOG_INTERVAL.toMillis()) {
            return;
        }
        LAST_FAILURE_LOG.put(throttleKey, now);
        if (cause == null) {
            log.warn("{} [source={}, key={}]", message, source, key);
        } else {
            log.error("{} [source={}, key={}]", message, source, key, cause);
        }
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
