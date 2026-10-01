package br.com.portalmanager.platform.library.authorization.visibility.springpoc;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizerGroup;
import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.authorization.visibility.ResourceVisibilityFilterManager;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the library contract in a real Spring Boot + JPA transaction:
 * @ResourceVisibility on the use case, a normal repository query, and a DTO return.
 */
@SpringBootTest(
        classes = ResourceVisibilitySpringJpaIntegrationTest.TestApplication.class,
        properties = {
                "platform.authorization.enabled=false",
                "platform.messaging.enabled=false",
                "spring.main.web-application-type=none",
                "spring.datasource.url=jdbc:h2:mem:resource_visibility_spring;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false"
        }
)
class ResourceVisibilitySpringJpaIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SpringPocWorkspaceQueryService service;

    @BeforeEach
    void seed() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("delete from spring_poc_workspace");
        jdbc.update(
                "insert into spring_poc_workspace (id, name, meu_authorizer) values (?, ?, ?)",
                1L, "Workspace BBS", "BBS-APP"
        );
        jdbc.update(
                "insert into spring_poc_workspace (id, name, meu_authorizer) values (?, ?, ?)",
                2L, "Workspace Catalog", "CATALOG"
        );
        jdbc.update(
                "insert into spring_poc_workspace (id, name, meu_authorizer) values (?, ?, ?)",
                3L, "Workspace Finance", "FINANCE"
        );
    }

    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    @Test
    void shouldFilterRepositoryInsideTransactionalUseCaseAndReturnDto() {
        UserContext.set(session("BBS-APP"));

        List<SpringPocWorkspaceOutput> first = service.findAll();

        assertEquals(
                List.of(new SpringPocWorkspaceOutput(1L, "Workspace BBS")),
                first
        );

        UserContext.set(session("CATALOG"));

        List<SpringPocWorkspaceOutput> second = service.findAll();

        assertEquals(
                List.of(new SpringPocWorkspaceOutput(2L, "Workspace Catalog")),
                second
        );
    }

    private static UserSession session(String authorizer) {
        UserSession session = new UserSession();
        session.setGroups(Set.of("USER"));
        session.setAuthorizerGroups(Set.of(
                new ParsedGroup(
                        "PM5-ENG-DEV_" + authorizer,
                        "ENG",
                        "DEV",
                        authorizer
                )
        ));
        return session;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = SpringPocWorkspace.class)
    @Import({
            SpringPocWorkspaceRepository.class,
            SpringPocWorkspaceQueryService.class
    })
    static class TestApplication {
    }
}

@Entity(name = "SpringPocWorkspace")
@Table(name = "spring_poc_workspace")
class SpringPocWorkspace {

    @Id
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @AuthorizerGroup
    @Column(name = "meu_authorizer", nullable = false)
    private String visibilityOwner;

    protected SpringPocWorkspace() {
    }

    Long id() {
        return id;
    }

    String name() {
        return name;
    }
}

record SpringPocWorkspaceOutput(Long id, String name) {
}

@Repository
class SpringPocWorkspaceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    List<SpringPocWorkspace> findFiltered() {
        assertTrue(
                TransactionSynchronizationManager.isActualTransactionActive(),
                "Repository query must execute inside the use-case transaction"
        );

        Session session = entityManager.unwrap(Session.class);
        assertNotNull(
                session.getEnabledFilter(ResourceVisibilityFilterManager.filterName(SpringPocWorkspace.class)),
                "Resource visibility filter must be enabled on the transaction-bound Hibernate Session"
        );

        return entityManager.createQuery(
                "select workspace from SpringPocWorkspace workspace order by workspace.id",
                SpringPocWorkspace.class
        ).getResultList();
    }
}

@Service
class SpringPocWorkspaceQueryService {

    private final SpringPocWorkspaceRepository repository;

    SpringPocWorkspaceQueryService(SpringPocWorkspaceRepository repository) {
        this.repository = repository;
    }

    @ResourceVisibility(SpringPocWorkspace.class)
    @Transactional(readOnly = true)
    public List<SpringPocWorkspaceOutput> findAll() {
        return repository.findFiltered().stream()
                .map(workspace -> new SpringPocWorkspaceOutput(workspace.id(), workspace.name()))
                .toList();
    }
}
