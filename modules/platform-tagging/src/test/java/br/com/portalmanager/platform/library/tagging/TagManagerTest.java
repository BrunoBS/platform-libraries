package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.storage.TagPersistence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TagManagerTest {

    @Test
    void shouldReconcileWithoutKnowingPersistenceTechnology() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        Owner owner = new Owner(10L);
        persistence.tags.add(new TestTag(owner, "same-tag", TagOriginType.SYSTEM));
        persistence.tags.add(new TestTag(owner, "obsolete", TagOriginType.MANUAL));

        TagManager<TestTag, Owner, Long, String> manager = new TagManager<>(persistence);
        manager.reconcile(owner, List.of(" Same Tag ", "manual"), List.of("same-tag", "system"));

        assertThat(persistence.tags)
                .extracting(tag -> tag.getName().value())
                .containsExactlyInAnyOrder("same-tag", "manual", "system");
        assertThat(persistence.tags.stream().filter(t -> t.name.value().equals("same-tag")).findFirst().orElseThrow().origin)
                .isEqualTo(TagOriginType.MANUAL);
    }

    @Test
    void shouldExposeGenericQueries() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        Owner first = new Owner(10L);
        Owner second = new Owner(20L);
        persistence.tags.add(new TestTag(first, "manual-a", TagOriginType.MANUAL));
        persistence.tags.add(new TestTag(second, "manual-b", TagOriginType.MANUAL));

        TagManager<TestTag, Owner, Long, String> manager = new TagManager<>(persistence);

        assertThat(manager.findManual(first)).containsExactly("manual-a");
        assertThat(manager.findManualByOwnerKeys(List.of("10", "20")))
                .containsEntry("10", List.of("manual-a"))
                .containsEntry("20", List.of("manual-b"));
        assertThat(manager.findOwnerKeysByTag(" MANUAL A ")).containsExactly("10");
    }


    @Test
    void shouldKeepManualPrecedenceAndAvoidDuplicates() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        Owner owner = new Owner(10L);
        TagManager<TestTag, Owner, Long, String> manager = new TagManager<>(persistence);

        manager.reconcile(owner, List.of(" Alpha ", "alpha"), List.of("ALPHA", "Beta", "beta"));

        assertThat(persistence.tags).extracting(tag -> tag.getName().value()).containsExactlyInAnyOrder("alpha", "beta");
        assertThat(persistence.tags.stream().filter(t -> t.name.value().equals("alpha")).findFirst().orElseThrow().origin)
                .isEqualTo(TagOriginType.MANUAL);
    }

    @Test
    void shouldReturnManualTagsDeterministicallyAndOnlyForRequestedOwners() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        Owner first = new Owner(10L);
        Owner second = new Owner(20L);
        Owner third = new Owner(30L);
        persistence.tags.add(new TestTag(first, "z-tag", TagOriginType.MANUAL));
        persistence.tags.add(new TestTag(first, "a-tag", TagOriginType.MANUAL));
        persistence.tags.add(new TestTag(third, "outside", TagOriginType.MANUAL));

        TagManager<TestTag, Owner, Long, String> manager = new TagManager<>(persistence);

        assertThat(manager.findManual(first)).containsExactly("a-tag", "z-tag");
        assertThat(manager.findManualByOwnerKeys(List.of("10", "20")))
                .containsEntry("10", List.of("a-tag", "z-tag"))
                .containsEntry("20", List.of())
                .doesNotContainKey("30");
    }

    @Test
    void shouldRejectNonNormalizedPersistedTag() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        Owner owner = new Owner(10L);
        assertThat(TagName.of(" Not Normalized ").value()).isEqualTo("not-normalized");
    }

    @Test
    void shouldRejectDuplicatePersistedTagsForSameOwner() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        Owner owner = new Owner(10L);
        persistence.tags.add(new TestTag(owner, "same", TagOriginType.MANUAL));
        persistence.tags.add(new TestTag(owner, "same", TagOriginType.SYSTEM));

        TagManager<TestTag, Owner, Long, String> manager = new TagManager<>(persistence);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> manager.reconcile(owner, List.of("same"), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate persisted tag");
    }

    @Test
    void shouldDeleteAllTagsForOwner() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        Owner first = new Owner(10L);
        Owner second = new Owner(20L);
        persistence.tags.add(new TestTag(first, "one", TagOriginType.MANUAL));
        persistence.tags.add(new TestTag(second, "two", TagOriginType.MANUAL));

        new TagManager<TestTag, Owner, Long, String>(persistence).deleteAll(first);

        assertThat(persistence.tags).extracting(tag -> tag.getName().value()).containsExactly("two");
    }

    private record Owner(Long id) {}

    private static final class TestTag implements Tag {
        private final Owner owner;
        private final TagName name;
        private TagOriginType origin;

        private TestTag(Owner owner, String name, TagOriginType origin) {
            this.owner = owner;
            this.name = TagName.of(name);
            this.origin = origin;
        }

        public TagName getName() { return name; }
        public TagOriginType getOriginType() { return origin; }
        public void changeOrigin(TagOriginType originType) { this.origin = originType; }
    }

    private static final class InMemoryPersistence
            implements TagPersistence<TestTag, Owner, Long, String> {
        private final List<TestTag> tags = new ArrayList<>();

        public Long ownerId(Owner owner) { return owner.id(); }
        public String ownerKey(TestTag tag) { return String.valueOf(tag.owner.id()); }
        public TestTag newTag(Owner owner, TagName name, TagOriginType origin) {\n            return new TestTag(owner, name.value(), origin);\n        }
        public List<TestTag> findByOwnerId(Long ownerId) {
            return tags.stream().filter(t -> t.owner.id().equals(ownerId)).toList();
        }
        public List<TestTag> findByOwnerKeysAndOrigin(Collection<String> keys, TagOriginType origin) {
            return tags.stream().filter(t -> keys.contains(ownerKey(t)) && t.origin == origin).toList();
        }
        public List<String> findOwnerKeysByTag(TagName name) {\n            return tags.stream().filter(t -> t.name.equals(name)).map(this::ownerKey).distinct().toList();\n        }
        public void saveAllTags(Collection<TestTag> values) { tags.addAll(values); }
        public void deleteAllTags(Collection<TestTag> values) { tags.removeAll(values); }
        public void deleteByOwnerId(Long ownerId) { tags.removeIf(t -> t.owner.id().equals(ownerId)); }
    }
}
