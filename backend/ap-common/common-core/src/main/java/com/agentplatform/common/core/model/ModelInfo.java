package com.agentplatform.common.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Unified model metadata returned by ModelRegistry (§3.9.2).
 * Represents both builtin and custom models.
 */
public class ModelInfo {

    public enum Source { BUILTIN, CUSTOM }

    private String id;
    private String name;
    private String provider;
    private Source source;
    private String description;
    private boolean isDefault;
    private boolean enabled;
    private Integer sortOrder;

    // custom-model only
    private String apiUrl;
    private String apiKeyMasked;
    private String connectionStatus;

    // parsed config map (provider-specific settings)
    private Map<String, Object> config;

    public ModelInfo() {}

    // ─── Getters / Setters ───

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getApiKeyMasked() { return apiKeyMasked; }
    public void setApiKeyMasked(String apiKeyMasked) { this.apiKeyMasked = apiKeyMasked; }

    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }

    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    // ─── equals / hashCode / toString ───

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelInfo that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ModelInfo{id='" + id + "', name='" + name + "', source=" + source + '}';
    }
}
