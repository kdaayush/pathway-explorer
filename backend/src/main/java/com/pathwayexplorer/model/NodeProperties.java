package com.pathwayexplorer.model;

public class NodeProperties {
    private String organism;
    private String description;
    private String source;

    public NodeProperties() {}

    public NodeProperties(String organism, String description, String source) {
        this.organism = organism;
        this.description = description;
        this.source = source;
    }

    public String getOrganism() { return organism; }
    public void setOrganism(String organism) { this.organism = organism; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
