package com.pathwayexplorer.model;

public class NetworkNode {
    private String id;
    private String label;
    private EntityType type;
    private NodeProperties properties;

    public NetworkNode() {}

    public NetworkNode(String id, String label, EntityType type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public EntityType getType() { return type; }
    public void setType(EntityType type) { this.type = type; }

    public NodeProperties getProperties() { return properties; }
    public void setProperties(NodeProperties properties) { this.properties = properties; }
}
