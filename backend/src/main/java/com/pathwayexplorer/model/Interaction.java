package com.pathwayexplorer.model;

public class Interaction {
    private String id;
    private InteractionEntity sourceEntity;
    private InteractionEntity targetEntity;
    private InteractionType type;
    private Float confidence;
    private DataSource source;
    private String description;

    public Interaction() {}

    public Interaction(String id, InteractionEntity sourceEntity, InteractionEntity targetEntity, InteractionType type) {
        this.id = id;
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public InteractionEntity getSourceEntity() { return sourceEntity; }
    public void setSourceEntity(InteractionEntity sourceEntity) { this.sourceEntity = sourceEntity; }

    public InteractionEntity getTargetEntity() { return targetEntity; }
    public void setTargetEntity(InteractionEntity targetEntity) { this.targetEntity = targetEntity; }

    public InteractionType getType() { return type; }
    public void setType(InteractionType type) { this.type = type; }

    public Float getConfidence() { return confidence; }
    public void setConfidence(Float confidence) { this.confidence = confidence; }

    public DataSource getSource() { return source; }
    public void setSource(DataSource source) { this.source = source; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
