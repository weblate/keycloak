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

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticatedClientSessionModel.SearchableFields;
import org.keycloak.models.map.common.StringKeyConverter.UUIDKey;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.models.map.storage.jpa.userSession.client.entity.JpaClientSessionEntity;
import org.keycloak.storage.SearchableModelField;

public class JpaClientSessionModelCriteriaBuilder extends JpaModelCriteriaBuilder<JpaClientSessionEntity, AuthenticatedClientSessionModel, JpaClientSessionModelCriteriaBuilder> {

    public JpaClientSessionModelCriteriaBuilder() {
        super(JpaClientSessionModelCriteriaBuilder::new);
    }

    private JpaClientSessionModelCriteriaBuilder(final BiFunction<CriteriaBuilder, Root<JpaClientSessionEntity>, Predicate> predicateFunc) {
        super(JpaClientSessionModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public JpaClientSessionModelCriteriaBuilder compare(SearchableModelField<? super AuthenticatedClientSessionModel> modelField, Operator op, Object... value) {
        switch(op) {
            case EQ:
                if (modelField == SearchableFields.ID ||
                    modelField == SearchableFields.USER_SESSION_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaClientSessionModelCriteriaBuilder((cb, root) -> {
                        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(Objects.toString(value[0], null));
                        if (uuid == null) return cb.or();
                        return cb.equal(root.get(modelField.getName()), uuid);
                    });
                } else if (modelField == SearchableFields.CLIENT_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaClientSessionModelCriteriaBuilder((cb, root) -> 
                        cb.equal(
                            cb.function("->", JsonbType.class, root.get("metadata"), cb.literal("fClientId")), 
                            cb.literal(convertToJson(value[0])))
                    );
                } else if (modelField == SearchableFields.IS_OFFLINE) {

                    validateValue(value, modelField, op, Boolean.class);

                    return new JpaClientSessionModelCriteriaBuilder((cb, root) -> 
                        cb.equal(root.get("offline"), value[0])
                    );
                } else if (modelField == SearchableFields.REALM_ID) {

                    validateValue(value, modelField, op, String.class);

                    return new JpaClientSessionModelCriteriaBuilder((cb, root) -> 
                        cb.equal(
                            cb.function("->", JsonbType.class, root.get("metadata"), cb.literal("fRealmId")), 
                            cb.literal(convertToJson(value[0])))
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case GT:
                if (modelField == SearchableFields.EXPIRATION) {
                    validateValue(value, modelField, op, Number.class);

                    return new JpaClientSessionModelCriteriaBuilder((cb, root) ->
                        cb.gt(root.get(modelField.getName()), (Number) value[0])
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }
}
