package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;
import br.com.portalmanager.platform.library.tagging.storage.TagOwnerTagName;
import br.com.portalmanager.platform.library.tagging.storage.TagRepository;
import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagManagerTest {

    @Test
    void shouldUsePlatformExceptionTypesForInvalidManagerConfiguration() {
        assertThatThrownBy(() -> new TagManager<TestTag, Owner>(null, TestTag::new))
                .isInstanceOf(PlatformConfigurationException.class);
        assertThatThrownBy(() -> new TagManager<TestTag, Owner>(repository(), null))
                .isInstanceOf(PlatformConfigurationException.class);
    }

    @Test
    void shouldUsePlatformValidationForInvalidTagState() {
        Owner owner = new Owner(10L, "workspace-10");

        assertThatThrownBy(() -> new TestTag(owner, null, TagOriginType.MANUAL))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new TestTag(owner, TagName.of("tag"), null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new TestTag(owner, TagName.of("tag"), TagOriginType.MANUAL)
                .changeOrigin(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectInvalidProjectionWithPlatformConfigurationException() {
        assertThatThrownBy(() -> new TagOwnerTagName(null, TagName.of("tag")))
                .isInstanceOf(PlatformConfigurationException.class);
        assertThatThrownBy(() -> new TagOwnerTagName("workspace-10", null))
                .isInstanceOf(PlatformConfigurationException.class);
    }

    @Test
    void shouldReconcileAndPersistOriginChangesExplicitly() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        TestTag existing = new TestTag(owner, TagName.of("same-tag"), TagOriginType.SYSTEM);

        when(repository.findByOwnerId(10L)).thenReturn(List.of(existing));

        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);
        manager.reconcile(owner, List.of(" Same Tag "));

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
        manager.reconcile(owner, List.of("new-tag"));

        verify(repository).deleteAll(List.of(obsolete));
        verify(repository).saveAll(any());
    }

    @Test
    void shouldReturnManualTagsByOwnerKeysUsingScalarProjection() {
        TagRepository<TestTag, Owner> repository = repository();
        when(repository.findOwnerTagNamesByIdentifiersAndOrigin(
                anyCollection(),
                eq(TagOriginType.MANUAL)
        )).thenReturn(List.of(
                new TagOwnerTagName("workspace-10", TagName.of("manual-a"))
        ));

        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);

        assertThat(manager.findManualByOwnerKeys(List.of(" workspace-10 ")))
                .containsEntry("workspace-10", List.of("manual-a"));
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
    void shouldReturnNoOwnersForTagThatNormalizesToEmpty() {
        TagRepository<TestTag, Owner> repository = repository();

        List<String> ownerKeys = new TagManager<TestTag, Owner>(repository, TestTag::new)
                .findOwnerKeysByTag("!!!");

        assertThat(ownerKeys).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(repository);
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

        assertThatThrownBy(() -> manager.reconcile(new Owner(10L, "   "), List.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectNullTagFromFactory() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        when(repository.findByOwnerId(10L)).thenReturn(List.of());
        TagManager<TestTag, Owner> manager = new TagManager<TestTag, Owner>(repository, (ignoredOwner, name, origin) -> null);

        assertThatThrownBy(() -> manager.reconcile(owner, List.of("tag-a")))
                .isInstanceOf(PlatformConfigurationException.class);
    }

    @Test
    void shouldPreferManualOverSystem() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10", List.of("shared"));
        when(repository.findByOwnerId(10L)).thenReturn(List.of());
        new TagManager<TestTag, Owner>(repository, TestTag::new)
                .reconcile(owner, List.of("shared"));
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(tags -> {
            List<TestTag> saved = new java.util.ArrayList<>();
            tags.forEach(saved::add);
            return saved.size() == 1 && saved.getFirst().getOriginType() == TagOriginType.MANUAL;
        }));
    }

    @Test
    void shouldDeduplicateNormalizedTags() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        when(repository.findByOwnerId(10L)).thenReturn(List.of());
        new TagManager<TestTag, Owner>(repository, TestTag::new)
                .reconcile(owner, List.of("My Tag", " my   tag ", "my-tag"));
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(tags -> {
            List<TestTag> saved = new java.util.ArrayList<>();
            tags.forEach(saved::add);
            return saved.size() == 1 && saved.getFirst().getName().equals(TagName.of("my-tag"));
        }));
    }

    @Test
    void shouldDeduplicateTagsAfterRemovingSpecialCharacters() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        when(repository.findByOwnerId(10L)).thenReturn(List.of());

        new TagManager<TestTag, Owner>(repository, TestTag::new)
                .reconcile(owner, List.of("work+space", "work@space", "workspace"));

        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(tags -> {
            List<TestTag> saved = new java.util.ArrayList<>();
            tags.forEach(saved::add);
            return saved.size() == 1 && saved.getFirst().getName().equals(TagName.of("workspace"));
        }));
    }

    @Test
    void shouldDeduplicateTagsAfterRemovingDiacritics() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        when(repository.findByOwnerId(10L)).thenReturn(List.of());

        new TagManager<TestTag, Owner>(repository, TestTag::new)
                .reconcile(owner, List.of("café", "cafe"));

        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(tags -> {
            List<TestTag> saved = new java.util.ArrayList<>();
            tags.forEach(saved::add);
            return saved.size() == 1 && saved.getFirst().getName().equals(TagName.of("cafe"));
        }));
    }

    @Test
    void shouldRejectDuplicatePersistedTags() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10");
        when(repository.findByOwnerId(10L)).thenReturn(List.of(
                new TestTag(owner, TagName.of("same"), TagOriginType.MANUAL),
                new TestTag(owner, TagName.of("same"), TagOriginType.SYSTEM)));
        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);
        assertThatThrownBy(() -> manager.reconcile(owner, List.of("same")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldDeleteAllByOwnerId() {
        TagRepository<TestTag, Owner> repository = repository();
        new TagManager<TestTag, Owner>(repository, TestTag::new)
                .deleteAll(new Owner(10L, "workspace-10"));
        verify(repository).deleteByOwnerId(10L);
    }

    @Test
    void shouldCreateSystemTagsFromOwnerWhenCreatingInitialTags() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-active", List.of("active"));
        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);

        assertThat(manager.createInitialTags(owner, List.of("manual")))
                .containsExactly("manual");
        verify(repository, never()).findByOwnerId(any());
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(tags -> {
            List<TestTag> saved = new java.util.ArrayList<>();
            tags.forEach(saved::add);
            return saved.size() == 2
                    && saved.stream().anyMatch(tag -> tag.getName().equals(TagName.of("active"))
                    && tag.getOriginType() == TagOriginType.SYSTEM)
                    && saved.stream().anyMatch(tag -> tag.getName().equals(TagName.of("manual"))
                    && tag.getOriginType() == TagOriginType.MANUAL);
        }));
    }

    @Test
    void shouldCalculateSystemTagsAndRemoveObsoleteOnReconciliation() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-active", List.of("active"));
        TestTag obsoleteSystemTag = new TestTag(owner, TagName.of("previous-state"), TagOriginType.SYSTEM);
        when(repository.findByOwnerId(10L)).thenReturn(List.of(obsoleteSystemTag));
        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);

        manager.reconcile(owner, List.of("manual"));

        verify(repository).deleteAll(List.of(obsoleteSystemTag));
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(tags -> {
            List<TestTag> saved = new java.util.ArrayList<>();
            tags.forEach(saved::add);
            return saved.size() == 2
                    && saved.stream().anyMatch(tag -> tag.getName().equals(TagName.of("active"))
                    && tag.getOriginType() == TagOriginType.SYSTEM)
                    && saved.stream().anyMatch(tag -> tag.getName().equals(TagName.of("manual"))
                    && tag.getOriginType() == TagOriginType.MANUAL);
        }));
    }

    @Test
    void shouldRejectNullSystemTagsFromOwner() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10", null);
        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);

        assertThatThrownBy(() -> manager.createInitialTags(owner, List.of()))
                .isInstanceOf(PlatformConfigurationException.class);
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void shouldIgnoreNullAndEmptySystemTagsAfterNormalization() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10",
                java.util.Arrays.asList(null, "", "   ", "!!!"));
        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);

        assertThat(manager.createInitialTags(owner, List.of())).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void shouldCreateInitialTagsWithoutReadingPersistedTags() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10", List.of("system", "a-tag"));
        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);
        assertThat(manager.createInitialTags(owner,
                java.util.Arrays.asList(" Z Tag ", "a tag", "A Tag", " ", "!!!", null)))
                .containsExactly("a-tag", "z-tag");
        verify(repository, never()).findByOwnerId(any());
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(tags -> {
            List<TestTag> saved = new java.util.ArrayList<>();
            tags.forEach(saved::add);
            return saved.size() == 3 && saved.stream().filter(tag -> tag.getOriginType() == TagOriginType.MANUAL).count() == 2;
        }));
    }

    @Test
    void shouldAvoidRepositoryCallsForEmptyInitialTags() {
        TagRepository<TestTag, Owner> repository = repository();
        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);
        assertThat(manager.createInitialTags(new Owner(10L, "workspace-10"), null)).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(repository);
        assertThatThrownBy(() -> manager.createInitialTags(new Owner(null, "workspace-10"), List.of("tag")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldReturnManualTagsAfterReconciliationWithOneRead() {
        TagRepository<TestTag, Owner> repository = repository();
        Owner owner = new Owner(10L, "workspace-10", List.of("system"));
        TestTag existing = new TestTag(owner, TagName.of("a-tag"), TagOriginType.SYSTEM);
        when(repository.findByOwnerId(10L)).thenReturn(List.of(existing));
        TagManager<TestTag, Owner> manager = new TagManager<>(repository, TestTag::new);
        assertThat(manager.reconcileAndReturnManualTags(owner, List.of("Z Tag", " A Tag ", "a-tag")))
                .containsExactly("a-tag", "z-tag");
        assertThat(existing.getOriginType()).isEqualTo(TagOriginType.MANUAL);
        verify(repository, org.mockito.Mockito.times(1)).findByOwnerId(10L);
    }

    @SuppressWarnings("unchecked")
    private static TagRepository<TestTag, Owner> repository() {
        return mock(TagRepository.class);
    }

    private record Owner(Long id, String identifier, List<String> systemTags) implements TagOwner {
        private Owner(Long id, String identifier) {
            this(id, identifier, List.of());
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public List<String> getSystemTags() {
            return systemTags;
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
