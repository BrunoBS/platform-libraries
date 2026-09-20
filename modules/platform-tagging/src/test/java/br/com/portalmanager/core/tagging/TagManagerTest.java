package br.com.portalmanager.core.tagging;

import br.com.portalmanager.core.tagging.model.Tag;
import br.com.portalmanager.core.tagging.model.TagOriginType;
import br.com.portalmanager.core.tagging.model.TagOwnerType;
import br.com.portalmanager.core.tagging.repository.TagRepository;
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
        TagRepository repository = repositoryWith();
        TagManager manager = new TagManager(repository);

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
        TagRepository repository = repositoryWith(existing);
        TagManager manager = new TagManager(repository);

        List<Tag> result = manager.reconcile(ACCOUNT, 10L, List.of("same-tag"), List.of("same-tag"));

        assertThat(result).singleElement().isSameAs(existing);
        assertThat(existing.getOriginType()).isEqualTo(TagOriginType.MANUAL);
        verify(repository, never()).deleteAll(anyList());
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void systemTagShouldReturnWhenManualOverrideIsRemovedWithoutChangingId() {
        Tag existing = tag("same-tag", TagOriginType.MANUAL);
        TagRepository repository = repositoryWith(existing);
        TagManager manager = new TagManager(repository);

        List<Tag> result = manager.reconcile(ACCOUNT, 10L, List.of(), List.of("same-tag"));

        assertThat(result).singleElement().isSameAs(existing);
        assertThat(existing.getOriginType()).isEqualTo(TagOriginType.SYSTEM);
        verify(repository, never()).deleteAll(anyList());
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void shouldDeleteOnlyObsoleteTags() {
        Tag kept = tag("keep", TagOriginType.SYSTEM);
        Tag obsolete = tag("remove", TagOriginType.MANUAL);
        TagRepository repository = repositoryWith(kept, obsolete);
        TagManager manager = new TagManager(repository);

        manager.reconcile(ACCOUNT, 10L, List.of(), List.of("keep"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).deleteAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(obsolete);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void shouldInsertOnlyNewTags() {
        Tag existing = tag("existing", TagOriginType.SYSTEM);
        TagRepository repository = repositoryWith(existing);
        TagManager manager = new TagManager(repository);

        List<Tag> result = manager.reconcile(
                ACCOUNT,
                10L,
                List.of("new-manual"),
                List.of("existing", "new-system"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(Tag::getName)
                .containsExactly("new-manual", "new-system");
        assertThat(result).hasSize(3);
        verify(repository, never()).deleteAll(anyList());
    }

    @Test
    void unchangedTagsShouldNotBeWritten() {
        Tag existing = tag("existing", TagOriginType.SYSTEM);
        TagRepository repository = repositoryWith(existing);
        TagManager manager = new TagManager(repository);

        List<Tag> result = manager.reconcile(ACCOUNT, 10L, List.of(), List.of("existing"));

        assertThat(result).containsExactly(existing);
        verify(repository, never()).deleteAll(anyList());
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void shouldNormalizeOwnerTypeAndTagNames() {
        TagRepository repository = repositoryWith();
        TagManager manager = new TagManager(repository);
        TagOwnerType owner = () -> " account ";

        List<Tag> result = manager.reconcile(
                owner,
                " 10 ",
                List.of(" Minha   Tag ", "minha tag"),
                List.of());

        verify(repository).findByOwnerTypeAndOwnerIdOrderByNameAsc("ACCOUNT", "10");
        assertThat(result).singleElement().satisfies(tag -> {
            assertThat(tag.getOwnerType()).isEqualTo("ACCOUNT");
            assertThat(tag.getOwnerId()).isEqualTo("10");
            assertThat(tag.getName()).isEqualTo("minha-tag");
            assertThat(tag.getOriginType()).isEqualTo(TagOriginType.MANUAL);
        });
    }

    @Test
    void shouldReadOnlyManualTagNames() {
        TagRepository repository = repositoryWith();
        when(repository.findByOwnerTypeAndOwnerIdAndOriginTypeOrderByNameAsc(
                "ACCOUNT", "10", TagOriginType.MANUAL))
                .thenReturn(List.of(tag("manual-a", TagOriginType.MANUAL), tag("manual-b", TagOriginType.MANUAL)));

        TagManager manager = new TagManager(repository);

        assertThat(manager.findManual(ACCOUNT, 10L)).containsExactly("manual-a", "manual-b");
    }

    @Test
    void shouldReadOnlySystemTagNames() {
        TagRepository repository = repositoryWith();
        when(repository.findByOwnerTypeAndOwnerIdAndOriginTypeOrderByNameAsc(
                "ACCOUNT", "10", TagOriginType.SYSTEM))
                .thenReturn(List.of(tag("system", TagOriginType.SYSTEM)));

        TagManager manager = new TagManager(repository);

        assertThat(manager.findSystem(ACCOUNT, 10L)).containsExactly("system");
    }

    @Test
    void shouldReadManualTagsForMultipleOwnersInOneQuery() {
        TagRepository repository = repositoryWith();
        Tag first = new Tag(ACCOUNT, "10", "first", TagOriginType.MANUAL);
        Tag second = new Tag(ACCOUNT, "20", "second", TagOriginType.MANUAL);
        when(repository.findByOwnerTypeAndOwnerIdInAndOriginTypeOrderByOwnerIdAscNameAsc(
                "ACCOUNT", java.util.Set.of("10", "20"), TagOriginType.MANUAL))
                .thenReturn(List.of(first, second));

        TagManager manager = new TagManager(repository);

        Map<String, List<String>> result = manager.findManualByOwners(ACCOUNT, List.of(10L, 20L));

        assertThat(result.get("10")).containsExactly("first");
        assertThat(result.get("20")).containsExactly("second");
    }

    private static TagRepository repositoryWith(Tag... tags) {
        TagRepository repository = mock(TagRepository.class);
        when(repository.findByOwnerTypeAndOwnerIdOrderByNameAsc("ACCOUNT", "10"))
                .thenReturn(List.of(tags));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        return repository;
    }

    private static Tag tag(String name, TagOriginType origin) {
        return new Tag(ACCOUNT, 10L, name, origin);
    }
}
