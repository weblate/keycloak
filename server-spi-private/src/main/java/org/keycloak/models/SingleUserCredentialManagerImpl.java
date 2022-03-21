/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models;

import org.keycloak.common.util.reflections.Types;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class SingleUserCredentialManagerImpl implements SingleUserCredentialManager  {

    private final UserModel user;
    private final KeycloakSession session;
    private final RealmModel realm;

    public SingleUserCredentialManagerImpl(KeycloakSession session, RealmModel realm, UserModel user) {
        this.user = user;
        this.session = session;
        this.realm = realm;
    }

    @Override
    public boolean isValid(List<CredentialInput> inputs) {
        if (!isValid(user)) {
            return false;
        }

        List<CredentialInput> toValidate = new LinkedList<>(inputs);

        validateCredentials(toValidate);

        getCredentialProviders(session, CredentialInputValidator.class)
                .forEach(validator -> validate(realm, user, toValidate, validator));

        return toValidate.isEmpty();
    }

    protected void validateCredentials(List<CredentialInput> toValidate) {
    }

    private boolean isValid(UserModel user) {
        return user != null && user.getServiceAccountClientLink() == null;
    }

    private void validate(RealmModel realm, UserModel user, List<CredentialInput> toValidate, CredentialInputValidator validator) {
        toValidate.removeIf(input -> validator.supportsCredentialType(input.getType()) && validator.isValid(realm, user, input));
    }

    private static <T> Stream<T> getCredentialProviders(KeycloakSession session, Class<T> type) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(CredentialProvider.class)
                .filter(f -> Types.supports(type, f, CredentialProviderFactory.class))
                .map(f -> (T) session.getProvider(CredentialProvider.class, f.getId()));
    }

}

