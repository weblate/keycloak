package org.keycloak.models.map.storage;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;

/**
 * Implementing this interface signals that the store can validate credentials.
 * This will be implemented, for example, by an LDAP store.
 *
 * @author Alexander Schwartz
 */
public interface MapStorageWithAuth<V extends AbstractEntity & UpdatableEntity, M> extends MapStorage<V, M> {

    boolean supportsCredentialType(String type);

    @Override
    MapKeycloakTransactionWithAuth<V, M> createTransaction(KeycloakSession session);
}
