package br.com.portalmanager.platform.library.testing.cloud.azure;

/** Default images used by the Azure emulator fixtures. */
public final class AzureContainerImages {
    public static final String AZURITE = "mcr.microsoft.com/azure-storage/azurite:3.37.0";
    public static final String AZURE_SERVICE_BUS = "mcr.microsoft.com/azure-messaging/servicebus-emulator:1.1.2";
    public static final String AZURE_SQL_SERVER = "mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04";

    private AzureContainerImages() {
    }
}
