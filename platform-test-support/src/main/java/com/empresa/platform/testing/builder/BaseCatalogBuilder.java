package com.empresa.platform.testing.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;

public abstract class BaseCatalogBuilder<D, B extends BaseCatalogBuilder<D, B>> {

    protected final Faker faker = new Faker();
    protected final ObjectMapper mapper = new ObjectMapper();

    protected Long id;
    protected String name;
    protected String label = faker.company().name();
    protected String description = faker.lorem().sentence();
    protected Integer sortOrder = faker.number().numberBetween(1, 10);
    protected JsonNode settings = mapper.createObjectNode();

    @SuppressWarnings("unchecked")
    protected B self() {
        return (B) this;
    }

    public B withId(Long id) {
        this.id = id;
        return self();
    }

    public B withName(String name) {
        this.name = name;
        return self();
    }

    public B withLabel(String label) {
        this.label = label;
        return self();
    }

    public B withDescription(String description) {
        this.description = description;
        return self();
    }

    public B withSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        return self();
    }

    public B withSettings(String json) {
        try {
            this.settings = json == null ? mapper.createObjectNode() : mapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JSON supplied to the test builder", exception);
        }
        return self();
    }

    public abstract D build();
}
