package com.empresa.platform.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseCatalogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    protected String name;

    @Column(name = "label", nullable = false, length = 100)
    protected String label;

    @Column(name = "description", columnDefinition = "TEXT")
    protected String description;

    @Column(name = "sort_order", nullable = false)
    protected Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    protected boolean active;

    @Column(name = "settings", nullable = false, columnDefinition = "TEXT")
    protected String settings;

    protected BaseCatalogEntity() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getSettings() { return settings; }
    public void setSettings(String settings) { this.settings = settings; }
}
