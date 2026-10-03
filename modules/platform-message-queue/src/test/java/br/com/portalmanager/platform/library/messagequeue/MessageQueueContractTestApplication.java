package br.com.portalmanager.platform.library.messagequeue;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import(MessageQueueContractListeners.class)
public class MessageQueueContractTestApplication {
}
