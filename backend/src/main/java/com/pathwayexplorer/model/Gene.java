package com.pathwayexplorer.model;

import java.util.ArrayList;
import java.util.List;

public class Gene {
    private String id;
    private String name;
    private String description;
    private String organism;
    private String symbol;
    private String chromosomeLocation;
    private List<Pathway> pathways = new ArrayList<>();
    private List<Protein> proteins = new ArrayList<>();
    private List<Interaction> interactions = new ArrayList<>();

    public Gene() {}

    public Gene(String id, String name, String description, String organism) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.organism = organism;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrganism() { return organism; }
    public void setOrganism(String organism) { this.organism = organism; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getChromosomeLocation() { return chromosomeLocation; }
    public void setChromosomeLocation(String chromosomeLocation) { this.chromosomeLocation = chromosomeLocation; }

    public List<Pathway> getPathways() { return pathways; }
    public void setPathways(List<Pathway> pathways) { this.pathways = pathways; }

    public List<Protein> getProteins() { return proteins; }
    public void setProteins(List<Protein> proteins) { this.proteins = proteins; }

    public List<Interaction> getInteractions() { return interactions; }
    public void setInteractions(List<Interaction> interactions) { this.interactions = interactions; }
}
