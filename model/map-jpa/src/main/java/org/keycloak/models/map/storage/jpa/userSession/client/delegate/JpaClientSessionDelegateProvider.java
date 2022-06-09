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
package org.keycloak.models.map.storage.jpa.userSession.client.delegate;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.storage.jpa.JpaDelegateProvider;
import org.keycloak.models.map.storage.jpa.userSession.client.entity.JpaClientSessionEntity;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntityFields;

/**
 * A {@link DelegateProvider} implementation for {@link JpaClientSessionEntity}.
 *
 */
public class JpaClientSessionDelegateProvider extends JpaDelegateProvider<JpaClientSessionEntity> implements DelegateProvider<MapAuthenticatedClientSessionEntity> {

    private final EntityManager em;

    public JpaClientSessionDelegateProvider(final JpaClientSessionEntity delegate, final EntityManager em) {
        super(delegate);
        this.em = em;
    }

    @Override
    public MapAuthenticatedClientSessionEntity getDelegate(boolean isRead, Enum<? extends EntityField<MapAuthenticatedClientSessionEntity>> field, Object... parameters) {
        if (getDelegate().isMetadataInitialized()) return getDelegate();
        if (isRead) {
            if (field instanceof MapAuthenticatedClientSessionEntityFields) {
                switch ((MapAuthenticatedClientSessionEntityFields) field) {
                    case ID:
                    case USER_SESSION_ID:
                    case ACTION:
                    case OFFLINE:
                    case TIMESTAMP:
                    case EXPIRATION:
                        return getDelegate();

                    case NOTES:
                        CriteriaBuilder cb = em.getCriteriaBuilder();
                        CriteriaQuery<JpaClientSessionEntity> query = cb.createQuery(JpaClientSessionEntity.class);
                        Root<JpaClientSessionEntity> root = query.from(JpaClientSessionEntity.class);
                        root.fetch("notes", JoinType.LEFT);
                        query.select(root).where(cb.equal(root.get("id"), UUID.fromString(getDelegate().getId())));
                        setDelegate(em.createQuery(query).getSingleResult());
                        break;

                    default:
                        setDelegate(em.find(JpaClientSessionEntity.class, UUID.fromString(getDelegate().getId())));
                }
            } else {
                throw new IllegalStateException("Not a valid client session field: " + field);
            }
        } else {
            setDelegate(em.find(JpaClientSessionEntity.class, UUID.fromString(getDelegate().getId())));
        }
        return getDelegate();
    }
}
