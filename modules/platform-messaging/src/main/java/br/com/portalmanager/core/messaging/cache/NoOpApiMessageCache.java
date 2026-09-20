package br.com.portalmanager.core.messaging.cache;
import br.com.portalmanager.core.messaging.model.ApiMessage;
import java.util.Locale;
import java.util.Optional;
public class NoOpApiMessageCache implements ApiMessageCache {
 public Optional<ApiMessage> get(String messageKey,Locale locale){return Optional.empty();}
 public void put(ApiMessage message){}
 public void evict(String messageKey,Locale locale){}
}
