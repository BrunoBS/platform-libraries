package br.com.portalmanager.platform.library.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@MappedSuperclass
public abstract class CatalogEntity {

    @Id
    @Column(name = "code", nullable = false, length = 50, updatable = false)
    protected String code;

    @Column(name = "label", nullable = false, length = 100)
    protected String label;

    @Column(name = "description", columnDefinition = "TEXT")
    protected String description;

    @Column(name = "sort_order", nullable = false)
    protected Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    protected boolean active;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false, columnDefinition = "json")
    protected JsonNode settings;

    protected CatalogEntity() {
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public JsonNode getSettings() { return settings; }
    public void setSettings(JsonNode settings) { this.settings = settings; }
}
