package com.pathwayexplorer.model;

import java.util.List;

public class SearchResult {
    private List<Pathway> pathways;
    private List<Gene> genes;
    private List<Protein> proteins;
    private int totalCount;

    public SearchResult() {}

    public SearchResult(List<Pathway> pathways, List<Gene> genes, List<Protein> proteins) {
        this.pathways = pathways;
        this.genes = genes;
        this.proteins = proteins;
        this.totalCount = pathways.size() + genes.size() + proteins.size();
    }

    public List<Pathway> getPathways() { return pathways; }
    public void setPathways(List<Pathway> pathways) { this.pathways = pathways; }

    public List<Gene> getGenes() { return genes; }
    public void setGenes(List<Gene> genes) { this.genes = genes; }

    public List<Protein> getProteins() { return proteins; }
    public void setProteins(List<Protein> proteins) { this.proteins = proteins; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
}
