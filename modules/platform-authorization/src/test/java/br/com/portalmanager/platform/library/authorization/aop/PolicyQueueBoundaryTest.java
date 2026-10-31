package br.com.portalmanager.platform.library.authorization.aop;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.aop.*;
import br.com.portalmanager.platform.library.authorization.client.AuthorizationClient;
import br.com.portalmanager.platform.library.authorization.config.*;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesRegistry;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueConsumer;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PolicyQueueBoundaryTest {
    public static class Facade {
        final AtomicInteger effects=new AtomicInteger();
        @AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.CREATE,
            mechanism=AuthorizationMechanism.POLICY,policy="boundary")
        public void ingest(PolicyAuthorizationContext scope) {
            assertThat(UserContext.get().orElseThrow().getAccountId()).isEqualTo(scope.accountIdentifier());
            effects.incrementAndGet();
        }
    }
    public static class Listener {
        final Facade facade;
        Listener(Facade facade) { this.facade=facade; }
        public void receive(MessageQueueMessage<String> message) {
            facade.ingest(new PolicyAuthorizationContext(message.correlationId(),message.payload(),null,null));
        }
    }
    @Test void realAndMockAuthorizeOnlyInsideOfficialSqsDelivery() throws Exception {
        for (AuthorizationExecutionMode mode : AuthorizationExecutionMode.values()) {
            PlatformAuthorizationProperties properties=new PlatformAuthorizationProperties();
            properties.setMode(mode); properties.setTechnicalPrincipals(Set.of("technical"));
            properties.setEntryPolicies(Map.of("boundary",new AuthorizationEntryPolicy(Listener.class.getName()+"#receive",
                Facade.class.getName()+"#ingest","configured","https://sqs.example/account/queue","technical",AuthorizationAction.CREATE,
                AuthorizationLevel.ADM,Set.of(new AuthorizationEntryPolicy.Scope("allowed-account",null,null)))));
            EntryPolicyAuthorizer authorizer=new EntryPolicyAuthorizer(properties,List.of(()->SqsMessageQueueConsumer.currentDelivery()
                .map(delivery->new AuthorizationEntryOrigin(delivery.entryPoint(),delivery.destination(),delivery.queueUrl(),
                    delivery.messageId(),delivery.correlationId(),delivery.timestamp()))));
            Facade target=new Facade(); AspectJProxyFactory factory=new AspectJProxyFactory(target);
            AuthorizationClient authorizationClient=mock(AuthorizationClient.class);
            if(mode==AuthorizationExecutionMode.REAL) factory.addAspect(new AuthorizationFacadeAspect(authorizationClient,authorizer));
            else factory.addAspect(new MockAuthorizationFacadeAspect(properties,authorizer));
            Facade facade=factory.getProxy(); Listener listener=new Listener(facade);
            SqsClient sqs=mock(SqsClient.class); MessageQueueSerializer serializer=mock(MessageQueueSerializer.class);
            SqsMessageQueueConsumer consumer=new SqsMessageQueueConsumer(sqs,mock(MessageQueueListenerRegistry.class),
                mock(DestinationResolver.class),serializer,new MessageQueueProperties(),mock(QueueCapabilitiesRegistry.class));
            MessageQueueListenerRegistry.ListenerDefinition definition=new MessageQueueListenerRegistry.ListenerDefinition("configured",listener,
                Listener.class.getMethod("receive",MessageQueueMessage.class),String.class);
            Method process=SqsMessageQueueConsumer.class.getDeclaredMethod("process",MessageQueueListenerRegistry.ListenerDefinition.class,String.class,Message.class);
            process.setAccessible(true);
            Message received=Message.builder().messageId("transport-id").body("body").receiptHandle("receipt").build();
            when(serializer.deserialize("body",String.class)).thenReturn(new MessageQueueMessage<>("event","existing","1",Instant.now(),"correlation",Map.of(),"allowed-account"));
            assertThatThrownBy(()->facade.ingest(new PolicyAuthorizationContext("correlation","allowed-account",null,null)))
                .isInstanceOf(br.com.portalmanager.platform.library.authorization.exception.ForbiddenAccessException.class);
            process.invoke(consumer,definition,"https://sqs.example/account/queue",received);
            assertThat(target.effects).hasValue(1); verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
            assertThat(SqsMessageQueueConsumer.currentDelivery()).isEmpty(); assertThat(UserContext.get()).isEmpty();
            reset(sqs);
            when(serializer.deserialize("body",String.class)).thenReturn(new MessageQueueMessage<>("event","existing","1",Instant.now(),"correlation",Map.of("principal","technical"),"other-account"));
            process.invoke(consumer,definition,"https://sqs.example/account/queue",received);
            assertThat(target.effects).hasValue(1); verify(sqs,never()).deleteMessage(any(DeleteMessageRequest.class));
            assertThat(SqsMessageQueueConsumer.currentDelivery()).isEmpty(); assertThat(UserContext.get()).isEmpty();
            verifyNoInteractions(authorizationClient);
        }
    }
}
