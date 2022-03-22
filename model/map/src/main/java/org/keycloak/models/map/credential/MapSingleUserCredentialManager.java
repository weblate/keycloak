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

package org.keycloak.models.map.credential;

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.DefaultSingleUserCredentialManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.user.MapUserEntity;

import java.util.List;

public class MapSingleUserCredentialManager extends DefaultSingleUserCredentialManager {

    private final MapUserEntity entity;

    public MapSingleUserCredentialManager(KeycloakSession session, RealmModel realm, UserModel user, MapUserEntity entity) {
        super(session, realm, user);
        this.entity = entity;
    }

    protected void validateCredentials(List<CredentialInput> toValidate) {
        entity.validateCredentials(toValidate);
    }

}

