package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;
import br.com.portalmanager.platform.library.tagging.storage.TagRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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


    @Test
    void shouldRejectBlankOwnerIdentifier() {
        TagRepository<TestTag, Owner> repository = repository();
        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);

        assertThatThrownBy(() -> manager.reconcile(new Owner(10L, "   "), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identifier");
    }

    @Test
    void shouldRejectNullTagFromFactory() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        when(repository.findByOwnerId(10L)).thenReturn(List.of());
        TagManager<TestTag, Owner> manager = new TagManager<TestTag, Owner>(repository, (ignoredOwner, name, origin) -> null);

        assertThatThrownBy(() -> manager.reconcile(owner, List.of("tag-a"), List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("factory");
    }

    @Test
    void shouldPreferManualOverSystem() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        when(repository.findByOwnerId(10L)).thenReturn(List.of());
        new TagManager<TestTag, Owner>(repository, TestTag::new)
                .reconcile(owner, List.of("shared"), List.of("shared"));
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(tags ->
                tags.size() == 1 && tags.iterator().next().getOriginType() == TagOriginType.MANUAL));
    }

    @Test
    void shouldDeduplicateNormalizedTags() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        when(repository.findByOwnerId(10L)).thenReturn(List.of());
        new TagManager<TestTag, Owner>(repository, TestTag::new)
                .reconcile(owner, List.of("My Tag", " my   tag ", "my-tag"), List.of());
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(tags -> tags.size() == 1));
    }

    @Test
    void shouldRejectDuplicatePersistedTags() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        when(repository.findByOwnerId(10L)).thenReturn(List.of(
                new TestTag(owner, TagName.of("same"), TagOriginType.MANUAL),
                new TestTag(owner, TagName.of("same"), TagOriginType.SYSTEM)));
        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);
        assertThatThrownBy(() -> manager.reconcile(owner, List.of("same"), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate persisted tag");
    }

    @Test
    void shouldDeleteAllByOwnerId() {
        TagRepository<TestTag, Owner> repository = repository();
        new TagManager<TestTag, Owner>(repository, TestTag::new)
                .deleteAll(new Owner(10L, "workspace-10"));
        verify(repository).deleteByOwnerId(10L);
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
