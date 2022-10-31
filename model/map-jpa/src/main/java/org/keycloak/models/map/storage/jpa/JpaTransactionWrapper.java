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
package org.keycloak.models.map.storage.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import io.quarkus.narayana.jta.QuarkusTransaction;
import org.hibernate.internal.SessionImpl;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.ModelException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps an {@link EntityTransaction} as a {@link KeycloakTransaction} so it can be enlisted in {@link org.keycloak.models.KeycloakTransactionManager}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaTransactionWrapper implements KeycloakTransaction {

    private static final Logger logger = Logger.getLogger(JpaTransactionWrapper.class);

    private final EntityManagerFactory emf;
    private EntityManager em;
    private boolean weStartedTheTransaction;

    public JpaTransactionWrapper(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public void begin() {
        if (!QuarkusTransaction.isActive()) {
            QuarkusTransaction.begin();
            weStartedTheTransaction = true;
        }
        logger.tracef("tx %d: begin", hashCode());
        em = emf.createEntityManager();
        try {
            // TODO: When using JTA this is no longer necessary, as that disables autocommit
            Connection connection = em.unwrap(SessionImpl.class).connection();
            // In the Undertow setup, Hibernate sets the connection to non-autocommit, and in the Quarkus setup the XA transaction manager does this.
            // For the Quarkus setup without a XA transaction manager, we didn't find a way to have this setup automatically.
            // There is also no known option to configure this in the Agroal DB connection pool in a Quarkus setup:
            // While the connection pool supports it, it hasn't been exposed as a Quarkus configuration option.
            // At the same time, disabling autocommit is essential to keep the transactional boundaries of the application.
            // The failure we've seen is the failed unique constraints that are usually deferred (for example, for client attributes).
            // A follow-up issue to track this is here: https://github.com/keycloak/keycloak/issues/13222
            if (connection.getAutoCommit()) {
                connection.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new ModelException("unable to set non-auto-commit to false");
        }
    }

    @Override
    public void commit() {
        try {
            logger.tracef("tx %d: commit", hashCode());
            if (weStartedTheTransaction) {
                QuarkusTransaction.commit();
            }
        } catch(PersistenceException pe) {
            throw PersistenceExceptionConverter.convert(pe.getCause() != null ? pe.getCause() : pe);
        }
    }

    @Override
    public void rollback() {
        logger.tracef("tx %d: rollback", hashCode());
        if (weStartedTheTransaction) {
            QuarkusTransaction.rollback();
        }
    }

    @Override
    public void setRollbackOnly() {
        QuarkusTransaction.setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {
        return QuarkusTransaction.isRollbackOnly();
    }

    @Override
    public boolean isActive() {
        return QuarkusTransaction.isActive();
    }

    public EntityManager getEntityManager() {
        return em;
    }
}
