package br.com.portalmanager.platform.library.authorization.resource;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizerGroup;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.resource.beans.spi.ManagedBean;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hibernate 7 bootstrap hook for platform-managed resource visibility.
 *
 * <p>The contributor is discovered through Java ServiceLoader. It defines the
 * platform filter once and attaches it programmatically to every mapped entity
 * that exposes an {@link AuthorizerGroup} field. Domain entities therefore do
 * not need Hibernate @Filter/@FilterDef annotations or a marker interface.</p>
 */
public final class ResourceVisibilityAdditionalMappingContributor
        implements AdditionalMappingContributor {

    @Override
    public String getContributorName() {
        return "platform-resource-visibility";
    }

    @Override
    public void contribute(
            AdditionalMappingContributions contributions,
            InFlightMetadataCollector metadata,
            ResourceStreamLocator resourceStreamLocator,
            MetadataBuildingContext buildingContext
    ) {
        registerFilterDefinition(metadata);
        ResourceVisibilityMappingRegistrar.register(metadata);
    }

    private static void registerFilterDefinition(InFlightMetadataCollector metadata) {
        if (metadata.getFilterDefinition(ResourceVisibilityFilterManager.FILTER_NAME) != null) {
            return;
        }

        JdbcMapping stringType = metadata.getTypeConfiguration()
                .getBasicTypeRegistry()
                .getRegisteredType(String.class.getName());

        if (stringType == null) {
            throw new IllegalStateException(
                    "Hibernate String basic type is not registered for resource visibility"
            );
        }

        metadata.addFilterDefinition(
                new FilterDefinition(
                        ResourceVisibilityFilterManager.FILTER_NAME,
                        "",
                        false,
                        true,
                        Map.of(ResourceVisibilityFilterManager.PARAMETER_NAME, stringType),
                        Map.<String, ManagedBean<? extends Supplier<?>>>of()
                )
        );
    }
}
