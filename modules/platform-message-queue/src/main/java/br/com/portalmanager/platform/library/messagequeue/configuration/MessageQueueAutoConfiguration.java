package br.com.portalmanager.platform.library.messagequeue.configuration;

import br.com.portalmanager.platform.library.messagequeue.publisher.MessageEnvelopeFactory;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@AutoConfiguration
@EnableConfigurationProperties(MessageQueueProperties.class)
public class MessageQueueAutoConfiguration {

    @Bean
    Clock messageQueueClock() {
        return Clock.systemUTC();
    }

    @Bean
    DestinationResolver destinationResolver(MessageQueueProperties properties) {
        return new DestinationResolver(properties);
    }

    @Bean
    MessageEnvelopeFactory messageEnvelopeFactory(Clock messageQueueClock) {
        return new MessageEnvelopeFactory(messageQueueClock);
    }
}
