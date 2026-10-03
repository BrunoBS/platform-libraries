package br.com.portalmanager.platform.library.tagging.storage;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@Import(TagRepositoryJpaTest.JpaTestConfiguration.class)
class TagRepositoryJpaTest {

    @Autowired
    private TestTagRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldExecuteSharedJpaQueriesAndConverter() {
        TestOwner first = persistOwner("workspace-1");
        TestOwner second = persistOwner("workspace-2");

        repository.saveAll(List.of(
                new TestTag(first, TagName.of(" Manual A "), TagOriginType.MANUAL),
                new TestTag(first, TagName.of("system"), TagOriginType.SYSTEM),
                new TestTag(second, TagName.of("manual-a"), TagOriginType.MANUAL)
        ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByOwnerId(first.getId()))
                .extracting(tag -> tag.getName().value())
                .containsExactly("manual-a", "system");

        assertThat(repository.findByOwnerIdentifiersAndOrigin(
                List.of("workspace-1", "workspace-2"), TagOriginType.MANUAL))
                .extracting(tag -> tag.getName().value())
                .containsExactly("manual-a", "manual-a");

        assertThat(repository.findOwnerIdentifiersByTag(TagName.of(" MANUAL A ")))
                .containsExactly("workspace-1", "workspace-2");

        assertThat(repository.deleteByOwnerId(first.getId())).isEqualTo(2);
        entityManager.flush();

        assertThat(repository.findByOwnerId(first.getId())).isEmpty();
        assertThat(repository.findByOwnerId(second.getId())).hasSize(1);
    }

    private TestOwner persistOwner(String identifier) {
        TestOwner owner = new TestOwner(identifier);
        entityManager.persist(owner);
        entityManager.flush();
        return owner;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {TestOwner.class, TestTag.class})
    @EnableJpaRepositories(basePackageClasses = TestTagRepository.class)
    static class JpaTestConfiguration {
    }
}

interface TestTagRepository extends TagRepository<TestTag, TestOwner> {
}

@Entity(name = "TestTag")
@Table(name = "test_tag")
class TestTag extends Tag<TestOwner> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private TestOwner owner;

    protected TestTag() {
    }

    TestTag(TestOwner owner, TagName name, TagOriginType originType) {
        super(name, originType);
        this.owner = owner;
    }

    @Override
    public TestOwner getOwner() {
        return owner;
    }
}

@Entity(name = "TestOwner")
@Table(name = "test_owner")
class TestOwner implements TagOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identifier", nullable = false, unique = true, length = 36)
    private String identifier;

    protected TestOwner() {
    }

    TestOwner(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }
}
