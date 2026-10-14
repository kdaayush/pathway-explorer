package com.pathwayexplorer.model;

import java.util.ArrayList;
import java.util.List;

public class Pathway {
    private String id;
    private String name;
    private String description;
    private String organism;
    private DataSource source;
    private String url;
    private List<Gene> genes = new ArrayList<>();
    private String imageUrl;
    private String category;

    public Pathway() {}

    public Pathway(String id, String name, String description, String organism, DataSource source) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.organism = organism;
        this.source = source;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrganism() { return organism; }
    public void setOrganism(String organism) { this.organism = organism; }

    public DataSource getSource() { return source; }
    public void setSource(DataSource source) { this.source = source; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public List<Gene> getGenes() { return genes; }
    public void setGenes(List<Gene> genes) { this.genes = genes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
