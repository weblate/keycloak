package org.keycloak.storage;

import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.RoleProvider;
import org.keycloak.provider.Provider;

public interface DatastoreProvider extends Provider {

    public ClientScopeProvider clientScopes();

    public GroupProvider groups();

    public RoleProvider roles();
    
}
