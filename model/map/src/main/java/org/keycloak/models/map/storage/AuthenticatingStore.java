package org.keycloak.models.map.storage;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapCrudOperations;

/**
 * Implementing this interface signals that the store can validate credentials.
 * This will be implemented, for example, by an LDAP store.
 *
 * @author Alexander Schwartz
 */
public interface AuthenticatingStore<V extends AbstractEntity & UpdatableEntity, M> extends MapStorage<V, M>, ConcurrentHashMapCrudOperations<V, M> {

    boolean supportsCredentialAuthenticationFor(String type);

    @Override
    AuthenticatingTransaction<V, M> createTransaction(KeycloakSession session);
}
