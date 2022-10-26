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

package org.keycloak.testsuite.model.authz;

import org.jboss.logging.Logger;
import org.junit.Test;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

@RequireProvider(CachedStoreFactoryProvider.class)
@RequireProvider(RealmProvider.class)
@RequireProvider(ClientProvider.class)
public class ConcurrentAuthzTest extends KeycloakModelTest {

    private String realmId;
    private String resourceServerId;
    private String resourceId;
    private String adminId;

    private static final Logger LOG = Logger.getLogger(ConcurrentAuthzTest.class);

    @Override
    protected void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("test");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));

        realmId = realm.getId();

        ClientModel client = s.clients().addClient(realm, "my-server");

        AuthorizationProvider authorization = s.getProvider(AuthorizationProvider.class);
        StoreFactory aStore = authorization.getStoreFactory();

        ResourceServer rs = aStore.getResourceServerStore().create(client);
        resourceServerId = rs.getId();
        resourceId =  aStore.getResourceStore().create(rs, "myResource", client.getClientId()).getId();
        aStore.getScopeStore().create(rs, "read");

        adminId = s.users().addUser(realm, "admin").getId();
    }

    @Override
    protected void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Override
    protected boolean isUseSameKeycloakSessionFactoryForAllThreads() {
        return true;
    }

    @Test
    public void testPermissionRemoved() {
        IntStream.range(0, 500).parallel().forEach(index -> {
            String permissionId = withRealm(realmId, (session, realm) -> {
                AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
                StoreFactory aStore = authorization.getStoreFactory();
                ResourceServer rs = aStore.getResourceServerStore().findById(realm, resourceServerId);

                UserModel u = session.users().addUser(realm, "user" + index);

                UmaPermissionRepresentation permission = new UmaPermissionRepresentation();
                permission.setName(KeycloakModelUtils.generateId());
                permission.addUser(u.getUsername());
                permission.addScope("read");

                permission.addResource(resourceId);
                permission.setOwner(adminId);
                return aStore.getPolicyStore().create(rs, permission).getId();
            });

            withRealm(realmId, (session, realm) -> {
                AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
                StoreFactory aStore = authorization.getStoreFactory();

                aStore.getPolicyStore().delete(realm, permissionId);
                return null;
            });

            withRealm(realmId, (session, realm) -> {
                AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
                StoreFactory aStore = authorization.getStoreFactory();
                ResourceServer rs = aStore.getResourceServerStore().findById(realm, resourceServerId);

                Map<Policy.FilterOption, String[]> searchMap = new HashMap<>();
                searchMap.put(Policy.FilterOption.TYPE, new String[]{"uma"});
                searchMap.put(Policy.FilterOption.OWNER, new String[]{adminId});
                searchMap.put(Policy.FilterOption.PERMISSION, new String[] {"true"});
                Set<String> s = aStore.getPolicyStore().find(realm, rs, searchMap, 0, 500).stream().map(Policy::getId).collect(Collectors.toSet());
                assertThat(s, not(contains(permissionId)));
                return null;
            });
        });
    }

    @Test
    public void testStaleCacheConcurrent() throws ExecutionException, InterruptedException {
        String permissionId = withRealm(realmId, (session, realm) -> {
            AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
            StoreFactory aStore = authorization.getStoreFactory();
            UserModel u = session.users().getUserById(realm, adminId);
            ResourceServer rs = aStore.getResourceServerStore().findById(realm, resourceServerId);


            UmaPermissionRepresentation permission = new UmaPermissionRepresentation();
            permission.setName("Permission A");
            permission.addUser(u.getUsername());
            permission.addScope("read");

            permission.addResource(resourceId);
            permission.setOwner(adminId);
            return aStore.getPolicyStore().create(rs, permission).getId();
        });

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CompletableFuture allFutures = CompletableFuture.completedFuture(null);

        withRealm(realmId, (session, realm) -> {
            AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
            StoreFactory aStore = authorization.getStoreFactory();
            UserModel u = session.users().getUserById(realm, adminId);
            ResourceServer rs = aStore.getResourceServerStore().findById(realm, resourceServerId);

            aStore.getPolicyStore().findByResourceServer(rs).forEach(p -> System.out.println("In the beginning contains: " + p.getId() + " name " + p.getName()));
            return null;
        });


        for (int i = 0; i < 500; i++) {
            final int index = i;
            final AtomicReference<String> createdPolicyId = new AtomicReference<>();

            CountDownLatch created = new CountDownLatch(1);
            CompletableFuture future = CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    createdPolicyId.set(withRealm(realmId, (session, realm) -> {
                        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
                        StoreFactory aStore = authorization.getStoreFactory();
                        ResourceServer rs = aStore.getResourceServerStore().findById(realm, resourceServerId);
                        Policy permission = aStore.getPolicyStore().findById(realm, rs, permissionId);

                        UserPolicyRepresentation userRep = new UserPolicyRepresentation();
                        userRep.setName("isAdminUser" + index);
                        userRep.addUser("admin");
                        Policy associatedPolicy = aStore.getPolicyStore().create(rs, userRep);
                        LOG.infof("Creating %s with id %s", "isAdminUser" + index, associatedPolicy.getId());
                        permission.addAssociatedPolicy(associatedPolicy);
                        LOG.infof("In creating: %s", permission.getAssociatedPolicies().stream().map(p -> p.getId() + " - " + p.getName()).collect(Collectors.toList()));
                        return associatedPolicy.getId();
                    }));
                }
            }, executor).whenComplete(new BiConsumer<Void, Throwable>() {
                @Override
                public void accept(Void unused, Throwable throwable) {
                    if (throwable != null) {
                        throwable.printStackTrace();
                    }

                    created.countDown();
                }
            });

            CountDownLatch read = new CountDownLatch(1);
            CompletableFuture future2 = CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        created.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    withRealm(realmId, (session, realm) -> {

                        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
                        StoreFactory aStore = authorization.getStoreFactory();
                        ResourceServer rs = aStore.getResourceServerStore().findById(realm, resourceServerId);
                        Policy permission = aStore.getPolicyStore().findById(realm, rs, permissionId);

                        LOG.infof("In reading: %s", permission.getAssociatedPolicies().stream().map(p -> p.getId() + " - " + p.getName()).collect(Collectors.toList()));
                        ModelToRepresentation.toRepresentation(permission, authorization);

                        return null;
                    });
                }
            }, executor).whenComplete(new BiConsumer<Void, Throwable>() {
                @Override
                public void accept(Void unused, Throwable throwable) {
                    if (throwable != null) {
                        throwable.printStackTrace();
                    }

                    read.countDown();
                }
            });

            CompletableFuture future3 = CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        read.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    LOG.infof("In removal: %s", createdPolicyId.get());

                    withRealm(realmId, (session, realm) -> {
                        assertThat(createdPolicyId.get(), notNullValue());
                        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
                        StoreFactory aStore = authorization.getStoreFactory();
                        aStore.getPolicyStore().delete(realm, createdPolicyId.get());
                        return null;
                    });
                }
            }, executor).whenComplete(new BiConsumer<Void, Throwable>() {
                @Override
                public void accept(Void unused, Throwable throwable) {
                    if (throwable != null) {
                        throwable.printStackTrace();
                    }
                }
            });


            allFutures = CompletableFuture.allOf(allFutures, future, future2, future3);
        }
        allFutures.get();
        executor.shutdownNow();
    }
}
