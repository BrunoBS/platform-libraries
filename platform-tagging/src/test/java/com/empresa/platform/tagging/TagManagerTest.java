package com.empresa.platform.tagging;

import com.empresa.platform.tagging.model.Tag;
import com.empresa.platform.tagging.model.TagOriginType;
import com.empresa.platform.tagging.model.TagOwnerType;
import com.empresa.platform.tagging.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

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
