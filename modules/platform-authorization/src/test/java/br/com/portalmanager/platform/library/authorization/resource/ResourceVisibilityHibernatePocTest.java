package br.com.portalmanager.platform.library.authorization.resource;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizerGroup;
import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.authorization.aspect.ResourceVisibilityAspect;
import br.com.portalmanager.platform.library.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end POC for platform-managed Hibernate resource visibility.
 *
 * Acceptance criteria:
 * 1. entity has no Hibernate @Filter/@FilterDef annotations or marker interface;
 * 2. @AuthorizerGroup may use any Java attribute and physical column name;
 * 3. collection query is restricted by the annotated authorizer column;
 * 4. OWNER executes unrestricted;
 * 5. direct load/find-by-id is protected;
 * 6. concurrent Sessions never share filter parameters;
 * 7. filter is disabled/cleaned in finally.
 */
class ResourceVisibilityHibernatePocTest {

    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    @BeforeAll
    static void setUpHibernate() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("jakarta.persistence.jdbc.driver", "org.h2.Driver")
                .applySetting("jakarta.persistence.jdbc.url", "jdbc:h2:mem:resource_visibility;DB_CLOSE_DELAY=-1")
                .applySetting("jakarta.persistence.jdbc.user", "sa")
                .applySetting("jakarta.persistence.jdbc.password", "")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.show_sql", "false")
                .build();

        sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(PocAccount.class)
                .buildMetadata()
                .buildSessionFactory();

        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.persist(new PocAccount(1L, "Account BBS", "BBS-APP"));
            session.persist(new PocAccount(2L, "Account Catalog", "CATALOG"));
            session.persist(new PocAccount(3L, "Account Finance", "FINANCE"));
            transaction.commit();
        }
    }

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @AfterAll
    static void tearDownHibernate() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    void shouldFilterCollectionUsingAnnotatedPhysicalColumn() {
        try (Session session = sessionFactory.openSession()) {
            ResourceVisibilityFilterManager manager = new ResourceVisibilityFilterManager(session);
            manager.enable(Set.of("bbs-app", "CATALOG"));

            List<PocAccount> result = session.createQuery(
                    "from PocAccount account order by account.id",
                    PocAccount.class
            ).getResultList();

            assertEquals(List.of("BBS-APP", "CATALOG"), authorizers(result));
        }
    }

    @Test
    void shouldNotRestrictOwner() throws Throwable {
        try (Session session = sessionFactory.openSession()) {
            ResourceVisibilityFilterManager manager = new ResourceVisibilityFilterManager(session);
            ResourceVisibilityAspect aspect = new ResourceVisibilityAspect(manager);

            UserSession owner = new UserSession();
            owner.setGroups(Set.of("PM5_OWNER"));
            owner.setAuthorizerGroups(Set.of(
                    new ParsedGroup("PM5-ENG-DEV_BBS-APP", "ENG", "DEV", "BBS-APP")
            ));
            UserContext.set(owner);

            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            ResourceVisibility annotation = mock(ResourceVisibility.class);
            when(joinPoint.proceed()).thenAnswer(invocation -> session.createQuery(
                    "from PocAccount account order by account.id",
                    PocAccount.class
            ).getResultList());

            @SuppressWarnings("unchecked")
            List<PocAccount> result = (List<PocAccount>) aspect.applyVisibility(joinPoint, annotation);

            assertEquals(3, result.size());
            assertNull(session.getEnabledFilter(ResourceVisibilityFilterManager.FILTER_NAME));
        }
    }

    @Test
    void shouldProtectFindById() {
        try (Session session = sessionFactory.openSession()) {
            ResourceVisibilityFilterManager manager = new ResourceVisibilityFilterManager(session);
            manager.enable(Set.of("BBS-APP"));

            PocAccount allowed = session.find(PocAccount.class, 1L);
            PocAccount denied = session.find(PocAccount.class, 3L);

            assertEquals("BBS-APP", allowed.visibilityOwner);
            assertNull(denied);
        }
    }

    @Test
    void shouldIsolateVisibilityBetweenConcurrentSessions() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<List<String>> bbs = executor.submit(
                    () -> queryAuthorizersInOwnSession("BBS-APP", ready, start)
            );
            Future<List<String>> catalog = executor.submit(
                    () -> queryAuthorizersInOwnSession("CATALOG", ready, start)
            );

            ready.await();
            start.countDown();

            assertEquals(List.of("BBS-APP"), bbs.get());
            assertEquals(List.of("CATALOG"), catalog.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldCleanupFilterAfterInvocation() throws Throwable {
        try (Session session = sessionFactory.openSession()) {
            ResourceVisibilityFilterManager manager = new ResourceVisibilityFilterManager(session);
            ResourceVisibilityAspect aspect = new ResourceVisibilityAspect(manager);

            UserSession user = new UserSession();
            user.setGroups(Set.of("USER"));
            user.setAuthorizerGroups(Set.of(
                    new ParsedGroup("PM5-ENG-DEV_BBS-APP", "ENG", "DEV", "BBS-APP")
            ));
            UserContext.set(user);

            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            ResourceVisibility annotation = mock(ResourceVisibility.class);
            when(joinPoint.proceed()).thenAnswer(invocation -> {
                assertTrue(
                        session.getEnabledFilter(ResourceVisibilityFilterManager.FILTER_NAME) != null
                );
                return session.createQuery(
                        "from PocAccount account order by account.id",
                        PocAccount.class
                ).getResultList();
            });

            @SuppressWarnings("unchecked")
            List<PocAccount> result = (List<PocAccount>) aspect.applyVisibility(joinPoint, annotation);

            assertEquals(List.of("BBS-APP"), authorizers(result));
            assertNull(session.getEnabledFilter(ResourceVisibilityFilterManager.FILTER_NAME));
        }
    }

    private static List<String> queryAuthorizersInOwnSession(
            String authorizer,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        try (Session session = sessionFactory.openSession()) {
            ResourceVisibilityFilterManager manager = new ResourceVisibilityFilterManager(session);
            manager.enable(Set.of(authorizer));
            ready.countDown();
            start.await();

            return authorizers(session.createQuery(
                    "from PocAccount account order by account.id",
                    PocAccount.class
            ).getResultList());
        }
    }

    private static List<String> authorizers(List<PocAccount> accounts) {
        return accounts.stream()
                .map(account -> account.visibilityOwner)
                .toList();
    }

    @Entity(name = "PocAccount")
    @Table(name = "poc_account")
    static class PocAccount {

        @Id
        private Long id;

        @Column(name = "name", nullable = false)
        private String name;

        @AuthorizerGroup
        @Column(name = "custom_authorizer", nullable = false, length = 100)
        private String visibilityOwner;

        protected PocAccount() {
        }

        PocAccount(Long id, String name, String visibilityOwner) {
            this.id = id;
            this.name = name;
            this.visibilityOwner = visibilityOwner;
        }
    }
}
