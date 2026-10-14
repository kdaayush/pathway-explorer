package com.pathwayexplorer.model;

public class InteractionEntity {
    private String id;
    private String name;
    private EntityType type;

    public InteractionEntity() {}

    public InteractionEntity(String id, String name, EntityType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public EntityType getType() { return type; }
    public void setType(EntityType type) { this.type = type; }
}
