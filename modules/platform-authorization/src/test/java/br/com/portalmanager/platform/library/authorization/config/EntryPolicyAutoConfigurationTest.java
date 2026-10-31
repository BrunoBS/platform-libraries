package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.config.*;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.messagequeue.annotation.MessageQueueListener;
import br.com.portalmanager.platform.library.messagequeue.configuration.*;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueConsumer;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntryPolicyAutoConfigurationTest {
    public static class Facade {
        @AuthorizationRequired(level=AuthorizationLevel.ADM,action=AuthorizationAction.CREATE,
            mechanism=AuthorizationMechanism.POLICY,policy="ingest")
        public void ingest(PolicyAuthorizationContext scope) { }
    }
    public static class Listener {
        @MessageQueueListener("configured")
        public void receive(MessageQueueMessage<String> message) { }
    }
    ApplicationContextRunner base() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PlatformAuthorizationAutoConfiguration.class))
            .withInitializer(context->context.getEnvironment().setActiveProfiles("test"))
            .withPropertyValues("platform.authorization.mode=MOCK");
    }
    ApplicationContextRunner policy() {
        MessageQueueProperties properties=new MessageQueueProperties(); properties.setProvider(MessageQueueProvider.AWS);
        properties.getAws().setRegion("us-east-1"); MessageQueueProperties.Destination destination=new MessageQueueProperties.Destination();
        destination.setQueue("physical"); properties.getDestinations().put("configured",destination);
        return base().withBean(SqsMessageQueueConsumer.class,()->mock(SqsMessageQueueConsumer.class))
            .withBean(MessageQueueListenerRegistry.class,MessageQueueListenerRegistry::new)
            .withBean(DestinationResolver.class,()->new DestinationResolver(properties))
            .withBean(Facade.class,Facade::new).withBean(Listener.class,Listener::new)
            .withPropertyValues("platform.authorization.technical-principals[0]=technical",
                "platform.authorization.entry-policies.ingest.entry-point="+Listener.class.getName()+"#receive",
                "platform.authorization.entry-policies.ingest.facade="+Facade.class.getName()+"#ingest",
                "platform.authorization.entry-policies.ingest.destination=configured",
                "platform.authorization.entry-policies.ingest.queue-url=https://sqs.example/account/physical",
                "platform.authorization.entry-policies.ingest.principal=technical",
                "platform.authorization.entry-policies.ingest.action=CREATE",
                "platform.authorization.entry-policies.ingest.level=ADM",
                "platform.authorization.entry-policies.ingest.scopes[0].account-identifier=allowed-account");
    }
    @Test void tokenOnlyApplicationStartsWithoutQueueLibrary() {
        base().withClassLoader(new FilteredClassLoader("br.com.portalmanager.platform.library.messagequeue"))
            .run(context->{assertThat(context).hasNotFailed(); assertThat(context).doesNotHaveBean(AuthorizationEntryOriginResolver.class);});
    }
    @Test void validPolicyBindsAndUsesOptionalSqsBridge() {
        policy().run(context->{
            assertThat(context).hasNotFailed(); assertThat(context).hasSingleBean(AuthorizationEntryOriginResolver.class);
            assertThat(context.getBean(AuthorizationEntryOriginResolver.class).current()).isEmpty();
        });
    }
    @Test void policyWithoutConfigurationStopsStartup() {
        base().withBean(Facade.class,Facade::new).run(context->assertThat(context).hasFailed());
    }
    @Test void physicalDestinationMismatchStopsStartup() {
        policy().withPropertyValues("platform.authorization.entry-policies.ingest.queue-url=https://sqs.example/account/other")
            .run(context->assertThat(context).hasFailed());
    }
    @Test void unknownPrincipalStopsStartup() {
        policy().withPropertyValues("platform.authorization.entry-policies.ingest.principal=unknown")
            .run(context->assertThat(context).hasFailed());
    }
}
