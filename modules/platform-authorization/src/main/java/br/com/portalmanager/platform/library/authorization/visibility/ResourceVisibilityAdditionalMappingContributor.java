package br.com.portalmanager.platform.library.authorization.visibility;

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
 * <p>The contributor is discovered through Java ServiceLoader. It defines one
 * Hibernate filter per protected entity and attaches it programmatically to the
 * entity that exposes an {@link AuthorizerGroup} field.</p>
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
        JdbcMapping stringType = resolveStringType(metadata);

        for (Class<?> resourceType : ResourceVisibilityMappingRegistrar.findResourceTypes(metadata)) {
            registerFilterDefinition(metadata, resourceType, stringType);
        }

        ResourceVisibilityMappingRegistrar.register(metadata);
    }

    private static JdbcMapping resolveStringType(InFlightMetadataCollector metadata) {
        JdbcMapping stringType = metadata.getTypeConfiguration()
                .getBasicTypeRegistry()
                .getRegisteredType(String.class.getName());

        if (stringType == null) {
            throw new IllegalStateException(
                    "Hibernate String basic type is not registered for resource visibility"
            );
        }

        return stringType;
    }

    private static void registerFilterDefinition(
            InFlightMetadataCollector metadata,
            Class<?> resourceType,
            JdbcMapping stringType
    ) {
        String filterName = ResourceVisibilityFilterManager.filterName(resourceType);
        if (metadata.getFilterDefinition(filterName) != null) {
            return;
        }

        metadata.addFilterDefinition(
                new FilterDefinition(
                        filterName,
                        "",
                        false,
                        true,
                        Map.of(ResourceVisibilityFilterManager.PARAMETER_NAME, stringType),
                        Map.<String, ManagedBean<? extends Supplier<?>>>of()
                )
        );
    }
}
