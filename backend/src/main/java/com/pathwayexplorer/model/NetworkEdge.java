package com.pathwayexplorer.model;

public class NetworkEdge {
    private String id;
    private String source;
    private String target;
    private InteractionType type;
    private Float confidence;
    private String label;

    public NetworkEdge() {}

    public NetworkEdge(String id, String source, String target, InteractionType type) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public InteractionType getType() { return type; }
    public void setType(InteractionType type) { this.type = type; }

    public Float getConfidence() { return confidence; }
    public void setConfidence(Float confidence) { this.confidence = confidence; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
