/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.integration.web;

import static org.keycloak.services.resources.KeycloakApplication.getSessionFactory;

import java.util.function.Predicate;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.keycloak.quarkus.runtime.cli.ExecutionExceptionHandler;

/**
 * <p>This filter is responsible for managing the request lifecycle as well as setting up the necessary context to process incoming
 * requests.
 *
 * <p>The filter itself runs in a event loop and should delegate to worker threads any blocking code (for now, all requests are handled
 * as blocking).
 */
public class QuarkusRequestFilter implements Handler<RoutingContext> {

    private final static Logger logger = Logger.getLogger(QuarkusRequestFilter.class);

    private static final Handler<AsyncResult<Object>> EMPTY_RESULT = result -> {
        // we don't really care about the result because any exception thrown should be handled by the parent class
    };

    private Predicate<RoutingContext> contextFilter;

    public QuarkusRequestFilter() {
        this(null);
    }

    public QuarkusRequestFilter(Predicate<RoutingContext> contextFilter) {
        this.contextFilter = contextFilter;
    }

    @Override
    public void handle(RoutingContext context) {
        if (ignoreContext(context)) {
            context.next();
            return;
        }
        // our code should always be run as blocking until we don't provide a better support for running non-blocking code
        // in the event loop
        context.vertx().executeBlocking(createBlockingHandler(context), false, event -> {
            if (event.failed()) {
                logger.warn("execution failed", event.cause());
                if (!context.response().ended()) {
                    if (!context.response().headWritten()) {
                        unexpectedErrorResponse(context.response());
                    }
                    context.response().end().result();
                }
            }
        });
    }

    private boolean ignoreContext(RoutingContext context) {
        return contextFilter != null && contextFilter.test(context);
    }

    private Handler<Promise<Object>> createBlockingHandler(RoutingContext context) {
        return promise -> {
            KeycloakSessionFactory sessionFactory = getSessionFactory();
            KeycloakSession session = sessionFactory.create();

            configureContextualData(context, createClientConnection(context.request()), session);
            // avoid closing the session when headers are sent, as this could cause a remote call in RESTEasy's executor
            // configureEndHandler(context, session);

            KeycloakTransactionManager tx = session.getTransactionManager();

            try {
                tx.begin();
                context.next();
                promise.tryComplete();
            } catch (Throwable cause) {
                logger.warn("execution failed, rolling back transaction", cause);
                tx.setRollbackOnly();
                promise.tryFail(cause);
            } finally {
                close(session);
            }
        };
    }

    /**
     * Creates a handler to close the {@link KeycloakSession} before the response is written to response but after Resteasy
     * is done with processing its output.
     */
    private void configureEndHandler(RoutingContext context, KeycloakSession session) {
        context.addHeadersEndHandler(event -> {
            try {
                close(session);
            } catch (Throwable cause) {
                unexpectedErrorResponse(context.response());
            }
        });
    }

    private void unexpectedErrorResponse(HttpServerResponse response) {
        response.headers().clear();
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
        response.putHeader(HttpHeaderNames.CONTENT_LENGTH, "0");
        response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        response.putHeader(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        // writes an empty buffer to replace any data previously written
        response.write(Buffer.buffer(""));
    }

    private void configureContextualData(RoutingContext context, ClientConnection connection, KeycloakSession session) {
        Resteasy.pushContext(ClientConnection.class, connection);
        Resteasy.pushContext(KeycloakSession.class, session);
        // quarkus-resteasy changed and clears the context map before dispatching
        // need to push keycloak contextual objects into the routing context for retrieving it later
        context.put(KeycloakSession.class.getName(), session);
        context.put(ClientConnection.class.getName(), connection);
    }

    protected void close(KeycloakSession session) {
        KeycloakTransactionManager tx = session.getTransactionManager();

        try {
            if (tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }
        } finally {
            session.close();
        }
    }

    private ClientConnection createClientConnection(HttpServerRequest request) {
        return new ClientConnection() {
            @Override
            public String getRemoteAddr() {
                return request.remoteAddress().host();
            }

            @Override
            public String getRemoteHost() {
                return request.remoteAddress().host();
            }

            @Override
            public int getRemotePort() {
                return request.remoteAddress().port();
            }

            @Override
            public String getLocalAddr() {
                return request.localAddress().host();
            }

            @Override
            public int getLocalPort() {
                return request.localAddress().port();
            }
        };
    }
}
