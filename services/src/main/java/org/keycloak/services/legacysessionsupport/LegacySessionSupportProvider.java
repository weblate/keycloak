package org.keycloak.services.legacysessionsupport;

import org.keycloak.models.UserCredentialManager;
import org.keycloak.provider.Provider;

/**
 * @author Alexander Schwartz
 */
public interface LegacySessionSupportProvider extends Provider {

    @Deprecated
    UserCredentialManager userCredentialManager();
}
