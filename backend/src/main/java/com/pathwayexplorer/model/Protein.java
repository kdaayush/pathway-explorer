package com.pathwayexplorer.model;

import java.util.ArrayList;
import java.util.List;

public class Protein {
    private String id;
    private String accession;
    private String name;
    private String fullName;
    private String organism;
    private String sequence;
    private Integer length;
    private Integer molecularMass;
    private String function;
    private Gene gene;
    private List<String> geneNames = new ArrayList<>();
    private List<String> subcellularLocations = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private List<String> domains = new ArrayList<>();
    private List<CrossReference> crossReferences = new ArrayList<>();
    private String entryUrl;
    private DataSource source;

    public Protein() {}

    public Protein(String id, String accession, String name, String organism) {
        this.id = id;
        this.accession = accession;
        this.name = name;
        this.organism = organism;
        this.entryUrl = "https://www.uniprot.org/uniprotkb/" + accession + "/entry";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAccession() { return accession; }
    public void setAccession(String accession) {
        this.accession = accession;
        if (this.entryUrl == null || this.entryUrl.isEmpty()) {
            this.entryUrl = "https://www.uniprot.org/uniprotkb/" + accession + "/entry";
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getOrganism() { return organism; }
    public void setOrganism(String organism) { this.organism = organism; }

    public String getSequence() { return sequence; }
    public void setSequence(String sequence) { this.sequence = sequence; }

    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }

    public Integer getMolecularMass() { return molecularMass; }
    public void setMolecularMass(Integer molecularMass) { this.molecularMass = molecularMass; }

    public String getFunction() { return function; }
    public void setFunction(String function) { this.function = function; }

    public Gene getGene() { return gene; }
    public void setGene(Gene gene) { this.gene = gene; }

    public List<String> getGeneNames() { return geneNames; }
    public void setGeneNames(List<String> geneNames) { this.geneNames = geneNames; }

    public List<String> getSubcellularLocations() { return subcellularLocations; }
    public void setSubcellularLocations(List<String> subcellularLocations) { this.subcellularLocations = subcellularLocations; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<String> getDomains() { return domains; }
    public void setDomains(List<String> domains) { this.domains = domains; }

    public List<CrossReference> getCrossReferences() { return crossReferences; }
    public void setCrossReferences(List<CrossReference> crossReferences) { this.crossReferences = crossReferences; }

    public String getEntryUrl() { return entryUrl; }
    public void setEntryUrl(String entryUrl) { this.entryUrl = entryUrl; }

    public DataSource getSource() { return source; }
    public void setSource(DataSource source) { this.source = source; }
}
