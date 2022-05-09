package org.keycloak.models.cache.infinispan;

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.SingleEntityCredentialManager;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Alexander Schwartz
 */
public abstract class SingleEntityCredentialManagerCacheAdapter implements SingleEntityCredentialManager {

    private final SingleEntityCredentialManager singleEntityCredentialManager;

    protected SingleEntityCredentialManagerCacheAdapter(SingleEntityCredentialManager singleEntityCredentialManager) {
        this.singleEntityCredentialManager = singleEntityCredentialManager;
    }

    public abstract void invalidateCacheForUser();

    @Override
    public boolean isValid(List<CredentialInput> inputs) {
        // validating a password might still update its hashes, similar logic might apply to OTP logic
        // instead of having each
        invalidateCacheForUser();
        return singleEntityCredentialManager.isValid(inputs);
    }

    @Override
    public boolean updateCredential(CredentialInput input) {
        invalidateCacheForUser();
        return singleEntityCredentialManager.updateCredential(input);
    }

    @Override
    public void updateStoredCredential(CredentialModel cred) {
        invalidateCacheForUser();
        singleEntityCredentialManager.updateStoredCredential(cred);
    }

    @Override
    public CredentialModel createStoredCredential(CredentialModel cred) {
        invalidateCacheForUser();
        return singleEntityCredentialManager.createStoredCredential(cred);
    }

    @Override
    public boolean removeStoredCredentialById(String id) {
        invalidateCacheForUser();
        return singleEntityCredentialManager.removeStoredCredentialById(id);
    }

    @Override
    public CredentialModel getStoredCredentialById(String id) {
        return singleEntityCredentialManager.getStoredCredentialById(id);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream() {
        return singleEntityCredentialManager.getStoredCredentialsStream();
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
        return singleEntityCredentialManager.getStoredCredentialsByTypeStream(type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(String name, String type) {
        return singleEntityCredentialManager.getStoredCredentialByNameAndType(name, type);
    }

    @Override
    public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
        invalidateCacheForUser();
        return singleEntityCredentialManager.moveStoredCredentialTo(id, newPreviousCredentialId);
    }

    @Override
    public void updateCredentialLabel(String credentialId, String userLabel) {
        invalidateCacheForUser();
        singleEntityCredentialManager.updateCredentialLabel(credentialId, userLabel);
    }

    @Override
    public void disableCredentialType(String credentialType) {
        invalidateCacheForUser();
        singleEntityCredentialManager.disableCredentialType(credentialType);
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream() {
        return singleEntityCredentialManager.getDisableableCredentialTypesStream();
    }

    @Override
    public boolean isConfiguredFor(String type) {
        return singleEntityCredentialManager.isConfiguredFor(type);
    }

    @Override
    public boolean isConfiguredLocally(String type) {
        return singleEntityCredentialManager.isConfiguredLocally(type);
    }

    @Override
    public Stream<String> getConfiguredUserStorageCredentialTypesStream() {
        return singleEntityCredentialManager.getConfiguredUserStorageCredentialTypesStream();
    }

    @Override
    public CredentialModel createCredentialThroughProvider(CredentialModel model) {
        invalidateCacheForUser();
        return singleEntityCredentialManager.createCredentialThroughProvider(model);
    }

}
