/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.userSession.client;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.storage.jpa.Constants;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.userSession.client.delegate.JpaClientSessionDelegateProvider;
import org.keycloak.models.map.storage.jpa.userSession.client.entity.JpaClientSessionEntity;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntityDelegate;

public class JpaClientSessionMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaClientSessionEntity, MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> {

    public JpaClientSessionMapKeycloakTransaction(KeycloakSession session, final EntityManager em) {
        super(session, JpaClientSessionEntity.class, AuthenticatedClientSessionModel.class, em);
    }

    @Override
    protected Selection<? extends JpaClientSessionEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaClientSessionEntity> root) {
        return cb.construct(JpaClientSessionEntity.class,
                root.get("id"),
                root.get("version"),
                root.get("entityVersion"),
                root.get("userSessionId"),
                root.get("action"),
                root.get("offline"),
                root.get("timestamp"),
                root.get("expiration")
        );
    }

    @Override
    protected void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(Constants.CURRENT_SCHEMA_VERSION_CLIENT_SESSION);
    }

    @Override
    protected JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaClientSessionModelCriteriaBuilder();
    }

    @Override
    protected MapAuthenticatedClientSessionEntity mapToEntityDelegate(JpaClientSessionEntity original) {
        original.setEntityManager(em);
        return new MapAuthenticatedClientSessionEntityDelegate(new JpaClientSessionDelegateProvider(original, em));
    }

    @Override
    protected boolean lockingSupportedForEntity() {
        return true;
    }
}
