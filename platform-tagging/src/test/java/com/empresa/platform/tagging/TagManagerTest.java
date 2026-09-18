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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagManagerTest {

    private static final TagOwnerType ACCOUNT = () -> "ACCOUNT";

    @Test
    void manualTagShouldOverrideSystemTag() {
        TagRepository repository = mock(TagRepository.class);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        TagManager manager = new TagManager(repository);

        manager.reconcile(
                ACCOUNT,
                10L,
                List.of(" Minha Tag ", "manual"),
                List.of("minha   tag", "system"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<Tag> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved).anySatisfy(tag -> {
            assertThat(tag.getName()).isEqualTo("minha-tag");
            assertThat(tag.getOriginType()).isEqualTo(TagOriginType.MANUAL);
        });
        assertThat(saved).noneSatisfy(tag -> {
            assertThat(tag.getName()).isEqualTo("minha-tag");
            assertThat(tag.getOriginType()).isEqualTo(TagOriginType.SYSTEM);
        });
        verify(repository).deleteByOwnerTypeAndOwnerId("ACCOUNT", "10");
        verify(repository).flush();
    }

    @Test
    void systemTagShouldReturnWhenManualOverrideIsRemoved() {
        TagRepository repository = mock(TagRepository.class);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        TagManager manager = new TagManager(repository);

        List<Tag> saved = manager.reconcile(ACCOUNT, 10L, List.of(), List.of("same-tag"));

        assertThat(saved).singleElement().satisfies(tag -> {
            assertThat(tag.getName()).isEqualTo("same-tag");
            assertThat(tag.getOriginType()).isEqualTo(TagOriginType.SYSTEM);
        });
    }
}
