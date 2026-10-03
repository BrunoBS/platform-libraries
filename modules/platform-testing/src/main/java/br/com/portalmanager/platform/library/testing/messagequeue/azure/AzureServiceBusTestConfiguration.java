package br.com.portalmanager.platform.library.testing.messagequeue.azure;

import br.com.portalmanager.platform.library.testing.cloud.azure.AzureServiceBusContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class AzureServiceBusTestConfiguration {

    private static final String SERVICE_BUS_CONFIG = """
            {
              "UserConfig": {
                "Namespaces": [
                  {
                    "Name": "sbemulatorns",
                    "Queues": [
                      {
                        "Name": "platform-test-queue",
                        "Properties": {
                          "DeadLetteringOnMessageExpiration": false,
                          "DefaultMessageTimeToLive": "PT1H",
                          "DuplicateDetectionHistoryTimeWindow": "PT20S",
                          "ForwardDeadLetteredMessagesTo": "",
                          "ForwardTo": "",
                          "LockDuration": "PT1M",
                          "MaxDeliveryCount": 3,
                          "RequiresDuplicateDetection": false,
                          "RequiresSession": false
                        }
                      }
                    ],
                    "Topics": []
                  }
                ],
                "Logging": {
                  "Type": "File"
                }
              }
            }
            """;

    @Bean(initMethod = "start", destroyMethod = "stop")
    AzureServiceBusContainer azureServiceBusContainer() {
        return new AzureServiceBusContainer(SERVICE_BUS_CONFIG);
    }
}
