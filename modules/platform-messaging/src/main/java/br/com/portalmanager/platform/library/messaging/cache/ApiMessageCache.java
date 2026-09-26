package br.com.portalmanager.platform.library.messaging.cache;
import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import java.util.Locale;
import java.util.Optional;
public interface ApiMessageCache {
 Optional<ApiMessage> get(String messageKey,Locale locale);
 void put(ApiMessage message);
 void evict(String messageKey,Locale locale);
}
