package br.com.portalmanager.platform.library.authorization.resource;

/**
 * Marker contract for entities that participate in platform-managed resource visibility.
 *
 * <p>The entity does not need to expose an authorizer-group attribute. Visibility
 * is resolved externally and the Hibernate filter is applied against the entity id.</p>
 */
public interface AuthorizableResource {
}
