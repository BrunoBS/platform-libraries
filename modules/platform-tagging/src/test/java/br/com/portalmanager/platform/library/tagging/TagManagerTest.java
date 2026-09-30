package br.com.portalmanager.platform.library.tagging;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagRecord;
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
                .extracting(TestTag::getName)
                .containsExactlyInAnyOrder("same-tag", "manual", "system");
        assertThat(persistence.tags.stream().filter(t -> t.name.equals("same-tag")).findFirst().orElseThrow().origin)
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

    private record Owner(Long id) {}

    private static final class TestTag implements TagRecord {
        private final Owner owner;
        private final String name;
        private TagOriginType origin;

        private TestTag(Owner owner, String name, TagOriginType origin) {
            this.owner = owner;
            this.name = name;
            this.origin = origin;
        }

        public String getName() { return name; }
        public TagOriginType getOriginType() { return origin; }
        public void changeOrigin(TagOriginType originType) { this.origin = originType; }
    }

    private static final class InMemoryPersistence
            implements TagPersistence<TestTag, Owner, Long, String> {
        private final List<TestTag> tags = new ArrayList<>();

        public Long ownerId(Owner owner) { return owner.id(); }
        public String ownerKey(TestTag tag) { return String.valueOf(tag.owner.id()); }
        public TestTag newTag(Owner owner, String name, TagOriginType origin) {
            return new TestTag(owner, name, origin);
        }
        public List<TestTag> findByOwnerId(Long ownerId) {
            return tags.stream().filter(t -> t.owner.id().equals(ownerId)).toList();
        }
        public List<TestTag> findByOwnerKeysAndOrigin(Collection<String> keys, TagOriginType origin) {
            return tags.stream().filter(t -> keys.contains(ownerKey(t)) && t.origin == origin).toList();
        }
        public List<String> findOwnerKeysByTag(String normalizedTag) {
            return tags.stream().filter(t -> t.name.equals(normalizedTag)).map(this::ownerKey).distinct().toList();
        }
        public void saveAllTags(Collection<TestTag> values) { tags.addAll(values); }
        public void deleteAllTags(Collection<TestTag> values) { tags.removeAll(values); }
        public void deleteByOwnerId(Long ownerId) { tags.removeIf(t -> t.owner.id().equals(ownerId)); }
    }
}
