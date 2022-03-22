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

package org.keycloak.credential;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUserCredentialManager;
import org.keycloak.models.UserModel;

import java.util.List;

public class DefaultSingleUserCredentialManagerProvider implements SingleUserCredentialManagerProvider {

    private final KeycloakSession session;

    public DefaultSingleUserCredentialManagerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public SingleUserCredentialManager create(RealmModel realm, UserModel user) {
        return create(realm, user, new DefaultSingleUserCredentialManagerStrategy());
    }

    @Override
    public SingleUserCredentialManager create(RealmModel realm, UserModel user, SingleUserCredentialManagerStrategy strategy) {
        return new DefaultSingleUserCredentialManager(session, realm, user, strategy);
    }

    @Override
    public void close() {

    }

    private static class DefaultSingleUserCredentialManagerStrategy implements SingleUserCredentialManagerStrategy {
        @Override
        public void validateCredentials(List<CredentialInput> toValidate) {
        }

        @Override
        public boolean updateCredential(CredentialInput input) {
            return false;
        }
    }

}
