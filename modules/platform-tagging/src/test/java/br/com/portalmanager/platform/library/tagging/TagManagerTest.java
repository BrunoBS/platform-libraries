package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;
import br.com.portalmanager.platform.library.tagging.storage.TagRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagManagerTest {

    @Test
    void shouldReconcileAndPersistOriginChangesExplicitly() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        TestTag existing = new TestTag(owner, TagName.of("same-tag"), TagOriginType.SYSTEM);

        when(repository.findByOwnerId(10L)).thenReturn(List.of(existing));

        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);
        manager.reconcile(owner, List.of(" Same Tag "), List.of());

        assertThat(existing.getOriginType()).isEqualTo(TagOriginType.MANUAL);
        verify(repository).saveAll(List.of(existing));
        verify(repository, never()).deleteAll(any());
    }

    @Test
    void shouldCreateAndDeleteDuringReconciliation() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        TestTag obsolete = new TestTag(owner, TagName.of("obsolete"), TagOriginType.MANUAL);

        when(repository.findByOwnerId(10L)).thenReturn(List.of(obsolete));

        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);
        manager.reconcile(owner, List.of("new-tag"), List.of());

        verify(repository).deleteAll(List.of(obsolete));
        verify(repository).saveAll(any());
    }

    @Test
    void shouldNormalizeLookupInsideTagging() {
        TagRepository<TestTag, Owner> repository = repository();
        when(repository.findOwnerIdentifiersByTag(TagName.of("manual-a")))
                .thenReturn(List.of("workspace-10"));

        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);

        assertThat(manager.findOwnerKeysByTag(" MANUAL A ")).containsExactly("workspace-10");
    }

    @Test
    void shouldReturnManualTagsDeterministically() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        when(repository.findByOwnerId(10L)).thenReturn(List.of(
                new TestTag(owner, TagName.of("z-tag"), TagOriginType.MANUAL),
                new TestTag(owner, TagName.of("system"), TagOriginType.SYSTEM),
                new TestTag(owner, TagName.of("a-tag"), TagOriginType.MANUAL)
        ));

        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);

        assertThat(manager.findManual(owner)).containsExactly("a-tag", "z-tag");
    }

    @SuppressWarnings("unchecked")
    private static TagRepository<TestTag, Owner> repository() {
        return mock(TagRepository.class);
    }

    private record Owner(Long id, String identifier) implements TagOwner {
        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    private static final class TestTag extends Tag<Owner> {
        private final Owner owner;

        private TestTag(Owner owner, TagName name, TagOriginType originType) {
            super(name, originType);
            this.owner = owner;
        }

        @Override
        public Owner getOwner() {
            return owner;
        }
    }
}
