package br.com.portalmanager.platform.library.authorization.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.client.AuthorizationClient;
import br.com.portalmanager.platform.library.authorization.config.AuthorizationEntryPolicy;
import br.com.portalmanager.platform.library.authorization.config.AuthorizationExecutionMode;
import br.com.portalmanager.platform.library.authorization.config.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.config.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.config.AuthorizationMechanism;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.exception.ForbiddenAccessException;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationEntryOrigin;
import br.com.portalmanager.platform.library.authorization.model.PolicyAuthorizationContext;
import br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueDeliveryContext;
import br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueMessage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class PolicyQueueBoundaryTest {

    private static final String DESTINATION = "configured";
    private static final String QUEUE_URL = "https://sqs.example/account/queue";
    private static final String CORRELATION_ID = "correlation";
    private static final String MESSAGE_ID = "event";

    public static class Facade {
        final AtomicInteger effects = new AtomicInteger();

        @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE,
                mechanism = AuthorizationMechanism.POLICY, policy = "boundary")
        public void ingest(PolicyAuthorizationContext scope) {
            assertThat(UserContext.get().orElseThrow().getAccountId()).isEqualTo(scope.accountIdentifier());
            effects.incrementAndGet();
        }
    }

    public static class Listener {
        private final Facade facade;

        Listener(Facade facade) {
            this.facade = facade;
        }

        public void receive(MessageQueueMessage<String> message) {
            facade.ingest(new PolicyAuthorizationContext(
                    message.correlationId(), message.payload(), null, null));
        }
    }

    @Test
    void realAndMockAuthorizeOnlyInsideScopedDeliveryContext() throws Exception {
        for (AuthorizationExecutionMode mode : AuthorizationExecutionMode.values()) {
            PlatformAuthorizationProperties properties = new PlatformAuthorizationProperties();
            properties.setMode(mode);
            properties.setTechnicalPrincipals(Set.of("technical"));
            properties.setEntryPolicies(Map.of("boundary", new AuthorizationEntryPolicy(
                    Listener.class.getName() + "#receive",
                    Facade.class.getName() + "#ingest",
                    DESTINATION,
                    QUEUE_URL,
                    "technical",
                    AuthorizationAction.CREATE,
                    AuthorizationLevel.ADM,
                    Set.of(new AuthorizationEntryPolicy.Scope("allowed-account", null, null)))));

            EntryPolicyAuthorizer authorizer = new EntryPolicyAuthorizer(properties, List.of(() ->
                    MessageQueueDeliveryContext.current().map(delivery -> new AuthorizationEntryOrigin(
                            delivery.entryPoint(), delivery.destination(), delivery.resourceIdentifier(),
                            delivery.messageId(), delivery.correlationId(), delivery.timestamp()))));

            Facade target = new Facade();
            AspectJProxyFactory factory = new AspectJProxyFactory(target);
            AuthorizationClient authorizationClient = mock(AuthorizationClient.class);
            if (mode == AuthorizationExecutionMode.REAL) {
                factory.addAspect(new AuthorizationFacadeAspect(authorizationClient, authorizer));
            } else {
                factory.addAspect(new MockAuthorizationFacadeAspect(properties, authorizer));
            }
            Listener listener = new Listener(factory.getProxy());
            MessageQueueMessage<String> allowedMessage = message("allowed-account");

            assertThatThrownBy(() -> listener.receive(allowedMessage))
                    .isInstanceOf(ForbiddenAccessException.class);
            try (var ignored = deliveryContext()) {
                listener.receive(allowedMessage);
            }
            assertThat(target.effects).hasValue(1);
            assertThat(MessageQueueDeliveryContext.current()).isEmpty();
            assertThat(UserContext.get()).isEmpty();

            MessageQueueMessage<String> disallowedMessage = message("other-account");
            try (var ignored = deliveryContext()) {
                assertThatThrownBy(() -> listener.receive(disallowedMessage))
                        .isInstanceOf(ForbiddenAccessException.class);
            }
            assertThat(target.effects).hasValue(1);
            assertThat(MessageQueueDeliveryContext.current()).isEmpty();
            assertThat(UserContext.get()).isEmpty();
            verifyNoInteractions(authorizationClient);
        }
    }

    private static MessageQueueMessage<String> message(String account) {
        return new MessageQueueMessage<>("event", "existing", "1", Instant.now(), CORRELATION_ID,
                Map.of(), account);
    }

    private static MessageQueueDeliveryContext.Scope deliveryContext() {
        return MessageQueueDeliveryContext.open(new MessageQueueDeliveryContext.Delivery(
                "AWS", Listener.class.getName() + "#receive", DESTINATION, QUEUE_URL,
                "transport-id", MESSAGE_ID, CORRELATION_ID, Instant.now()));
    }
}
