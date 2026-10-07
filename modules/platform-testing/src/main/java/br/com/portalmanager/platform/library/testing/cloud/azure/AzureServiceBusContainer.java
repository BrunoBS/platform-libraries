package br.com.portalmanager.platform.library.testing.cloud.azure;

import org.testcontainers.azure.ServiceBusEmulatorContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;


public final class AzureServiceBusContainer implements AutoCloseable {

    public static final DockerImageName DEFAULT_SQL_SERVER_IMAGE = DockerImageName.parse(
            "mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04"
    );

    public static final DockerImageName DEFAULT_SERVICE_BUS_IMAGE = DockerImageName.parse(
            "mcr.microsoft.com/azure-messaging/servicebus-emulator:1.1.2"
    );

    private static final String DEFAULT_SQL_PASSWORD = "Platform_Testing_123!";

    private final Network network;
    private final MSSQLServerContainer sqlServer;
    private final ServiceBusEmulatorContainer emulator;

    public AzureServiceBusContainer(String configuration) {
        this(DEFAULT_SERVICE_BUS_IMAGE, DEFAULT_SQL_SERVER_IMAGE, configuration);
    }

    public AzureServiceBusContainer(
            DockerImageName serviceBusImage,
            DockerImageName sqlServerImage,
            String configuration) {
        if (configuration == null || configuration.isBlank()) {
            throw new IllegalArgumentException("Azure Service Bus emulator configuration is required");
        }

        this.network = Network.newNetwork();
        this.sqlServer = new MSSQLServerContainer(sqlServerImage)
                .acceptLicense()
                .withPassword(DEFAULT_SQL_PASSWORD)
                .withNetwork(network);
        this.emulator = new ServiceBusEmulatorContainer(serviceBusImage)
                .acceptLicense()
                .withConfig(Transferable.of(configuration))
                .withNetwork(network)
                .withMsSqlServerContainer(sqlServer);
    }

    public void start() {
        try {
            sqlServer.start();
            emulator.start();
        } catch (RuntimeException exception) {
            stop();
            throw exception;
        }
    }

    public void stop() {
        try {
            emulator.stop();
        } finally {
            try {
                sqlServer.stop();
            } finally {
                network.close();
            }
        }
    }

    public ServiceBusEmulatorContainer emulator() {
        return emulator;
    }

    @Override
    public void close() {
        stop();
    }
}
