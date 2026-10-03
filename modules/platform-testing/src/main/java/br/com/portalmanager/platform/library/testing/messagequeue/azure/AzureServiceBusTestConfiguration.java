package br.com.portalmanager.platform.library.testing.messagequeue.azure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.azure.ServiceBusEmulatorContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class AzureServiceBusTestConfiguration {

    static final DockerImageName SQL_SERVER_IMAGE = DockerImageName.parse(
            "mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04"
    );

    static final DockerImageName SERVICE_BUS_IMAGE = DockerImageName.parse(
            "mcr.microsoft.com/azure-messaging/servicebus-emulator:1.1.2"
    );

    private static final String SQL_PASSWORD = "Platform_Testing_123!";

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

    @Bean(destroyMethod = "close")
    Network azureServiceBusNetwork() {
        return Network.newNetwork();
    }

    @Bean(destroyMethod = "stop")
    MSSQLServerContainer azureServiceBusSqlServer(Network azureServiceBusNetwork) {
        return new MSSQLServerContainer(SQL_SERVER_IMAGE)
                .acceptLicense()
                .withPassword(SQL_PASSWORD)
                .withNetwork(azureServiceBusNetwork);
    }

    @Bean(destroyMethod = "stop")
    ServiceBusEmulatorContainer azureServiceBusContainer(
            Network azureServiceBusNetwork,
            MSSQLServerContainer azureServiceBusSqlServer) {
        return new ServiceBusEmulatorContainer(SERVICE_BUS_IMAGE)
                .acceptLicense()
                .withConfig(Transferable.of(SERVICE_BUS_CONFIG))
                .withNetwork(azureServiceBusNetwork)
                .withMsSqlServerContainer(azureServiceBusSqlServer);
    }
}
