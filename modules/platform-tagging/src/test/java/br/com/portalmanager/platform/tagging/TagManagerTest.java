package br.com.portalmanager.platform.tagging;

import br.com.portalmanager.platform.tagging.model.Tag;
import br.com.portalmanager.platform.tagging.model.TagOriginType;
import br.com.portalmanager.platform.tagging.model.TagOwnerType;
import br.com.portalmanager.platform.tagging.storage.TagStorage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagManagerTest {

    private static final TagOwnerType ACCOUNT = () -> "ACCOUNT";

    @Test
    void manualTagShouldOverrideSystemTag() {
        TagStorage storage = storageWith();
        TagManager manager = new TagManager(storage);

        List<Tag> result = manager.reconcile(
                ACCOUNT,
                10L,
                List.of(" Minha Tag ", "manual"),
                List.of("minha   tag", "system"));

        assertThat(result).hasSize(3);
        assertThat(result).anySatisfy(tag -> {
            assertThat(tag.getName()).isEqualTo("minha-tag");
            assertThat(tag.getOriginType()).isEqualTo(TagOriginType.MANUAL);
        });
        assertThat(result).noneSatisfy(tag -> {
            assertThat(tag.getName()).isEqualTo("minha-tag");
            assertThat(tag.getOriginType()).isEqualTo(TagOriginType.SYSTEM);
        });
    }

    @Test
    void shouldPromoteSystemTagToManualWithoutChangingId() {
        Tag existing = tag("same-tag", TagOriginType.SYSTEM);
        TagStorage storage = storageWith(existing);
        TagManager manager = new TagManager(storage);

        List<Tag> result = manager.reconcile(ACCOUNT, 10L, List.of("same-tag"), List.of("same-tag"));

        assertThat(result).singleElement().isSameAs(existing);
        assertThat(existing.getOriginType()).isEqualTo(TagOriginType.MANUAL);
        verify(storage, never()).deleteAll(anyList());
        verify(storage, never()).saveAll(anyList());
    }

    @Test
    void systemTagShouldReturnWhenManualOverrideIsRemovedWithoutChangingId() {
        Tag existing = tag("same-tag", TagOriginType.MANUAL);
        TagStorage storage = storageWith(existing);
        TagManager manager = new TagManager(storage);

        List<Tag> result = manager.reconcile(ACCOUNT, 10L, List.of(), List.of("same-tag"));

        assertThat(result).singleElement().isSameAs(existing);
        assertThat(existing.getOriginType()).isEqualTo(TagOriginType.SYSTEM);
        verify(storage, never()).deleteAll(anyList());
        verify(storage, never()).saveAll(anyList());
    }

    @Test
    void shouldDeleteOnlyObsoleteTags() {
        Tag kept = tag("keep", TagOriginType.SYSTEM);
        Tag obsolete = tag("remove", TagOriginType.MANUAL);
        TagStorage storage = storageWith(kept, obsolete);
        TagManager manager = new TagManager(storage);

        manager.reconcile(ACCOUNT, 10L, List.of(), List.of("keep"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(storage).deleteAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(obsolete);
        verify(storage, never()).saveAll(anyList());
    }

    @Test
    void shouldInsertOnlyNewTags() {
        Tag existing = tag("existing", TagOriginType.SYSTEM);
        TagStorage storage = storageWith(existing);
        TagManager manager = new TagManager(storage);

        List<Tag> result = manager.reconcile(
                ACCOUNT,
                10L,
                List.of("new-manual"),
                List.of("existing", "new-system"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(storage).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(Tag::getName)
                .containsExactly("new-manual", "new-system");
        assertThat(result).hasSize(3);
        verify(storage, never()).deleteAll(anyList());
    }

    @Test
    void unchangedTagsShouldNotBeWritten() {
        Tag existing = tag("existing", TagOriginType.SYSTEM);
        TagStorage storage = storageWith(existing);
        TagManager manager = new TagManager(storage);

        List<Tag> result = manager.reconcile(ACCOUNT, 10L, List.of(), List.of("existing"));

        assertThat(result).containsExactly(existing);
        verify(storage, never()).deleteAll(anyList());
        verify(storage, never()).saveAll(anyList());
    }

    @Test
    void shouldNormalizeOwnerTypeAndTagNames() {
        TagStorage storage = storageWith();
        TagManager manager = new TagManager(storage);
        TagOwnerType owner = () -> " account ";

        List<Tag> result = manager.reconcile(
                owner,
                " 10 ",
                List.of(" Minha   Tag ", "minha tag"),
                List.of());

        verify(storage).findByOwner("ACCOUNT", "10");
        assertThat(result).singleElement().satisfies(tag -> {
            assertThat(tag.getOwnerType()).isEqualTo("ACCOUNT");
            assertThat(tag.getOwnerId()).isEqualTo("10");
            assertThat(tag.getName()).isEqualTo("minha-tag");
            assertThat(tag.getOriginType()).isEqualTo(TagOriginType.MANUAL);
        });
    }

    @Test
    void shouldReadOnlyManualTagNames() {
        TagStorage storage = storageWith();
        when(storage.findByOwnerAndOrigin("ACCOUNT", "10", TagOriginType.MANUAL))
                .thenReturn(List.of(tag("manual-a", TagOriginType.MANUAL), tag("manual-b", TagOriginType.MANUAL)));

        TagManager manager = new TagManager(storage);

        assertThat(manager.findManual(ACCOUNT, 10L)).containsExactly("manual-a", "manual-b");
    }

    @Test
    void shouldReadOnlySystemTagNames() {
        TagStorage storage = storageWith();
        when(storage.findByOwnerAndOrigin("ACCOUNT", "10", TagOriginType.SYSTEM))
                .thenReturn(List.of(tag("system", TagOriginType.SYSTEM)));

        TagManager manager = new TagManager(storage);

        assertThat(manager.findSystem(ACCOUNT, 10L)).containsExactly("system");
    }

    @Test
    void shouldFindOwnerIdsByNormalizedTag() {
        TagStorage storage = storageWith();
        when(storage.findOwnerIdsByTag("ACCOUNT", "minha-tag"))
                .thenReturn(List.of("10", "20"));

        TagManager manager = new TagManager(storage);

        assertThat(manager.findOwnerIdsByTag(ACCOUNT, "  Minha   Tag  "))
                .containsExactly("10", "20");

        verify(storage).findOwnerIdsByTag("ACCOUNT", "minha-tag");
    }

    @Test
    void shouldNotQueryWhenSearchTagIsBlank() {
        TagStorage storage = storageWith();
        TagManager manager = new TagManager(storage);

        assertThat(manager.findOwnerIdsByTag(ACCOUNT, "   ")).isEmpty();

        verify(storage, never()).findOwnerIdsByTag(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldReadManualTagsForMultipleOwnersInOneQuery() {
        TagStorage storage = storageWith();
        Tag first = new Tag(ACCOUNT, "10", "first", TagOriginType.MANUAL);
        Tag second = new Tag(ACCOUNT, "20", "second", TagOriginType.MANUAL);
        when(storage.findByOwnersAndOrigin(
                "ACCOUNT", java.util.Set.of("10", "20"), TagOriginType.MANUAL))
                .thenReturn(List.of(first, second));

        TagManager manager = new TagManager(storage);

        Map<String, List<String>> result = manager.findManualByOwners(ACCOUNT, List.of(10L, 20L));

        assertThat(result.get("10")).containsExactly("first");
        assertThat(result.get("20")).containsExactly("second");
    }

    private static TagStorage storageWith(Tag... tags) {
        TagStorage storage = mock(TagStorage.class);
        when(storage.findByOwner("ACCOUNT", "10")).thenReturn(List.of(tags));
        when(storage.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        return storage;
    }

    private static Tag tag(String name, TagOriginType origin) {
        return new Tag(ACCOUNT, 10L, name, origin);
    }
}
