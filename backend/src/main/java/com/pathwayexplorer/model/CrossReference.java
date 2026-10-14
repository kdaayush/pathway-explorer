package com.pathwayexplorer.model;

public class CrossReference {
    private String database;
    private String id;
    private String description;
    private String url;

    public CrossReference() {}

    public CrossReference(String database, String id, String description, String url) {
        this.database = database;
        this.id = id;
        this.description = description;
        this.url = url;
    }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
