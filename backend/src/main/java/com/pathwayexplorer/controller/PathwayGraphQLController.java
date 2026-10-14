package com.pathwayexplorer.controller;

import com.pathwayexplorer.model.*;
import com.pathwayexplorer.service.*;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

/**
 * GraphQL controller for biological pathway queries.
 * Maps GraphQL query fields to service methods.
 */
@Controller
public class PathwayGraphQLController {

    private final KeggService keggService;
    private final ReactomeService reactomeService;
    private final UniProtService uniProtService;
    private final NetworkService networkService;

    public PathwayGraphQLController(KeggService keggService, ReactomeService reactomeService,
                                     UniProtService uniProtService, NetworkService networkService) {
        this.keggService = keggService;
        this.reactomeService = reactomeService;
        this.uniProtService = uniProtService;
        this.networkService = networkService;
    }

    /**
     * Unified search across all biological databases.
     */
    @QueryMapping
    public SearchResult search(@Argument String query, @Argument Integer limit) {
        int searchLimit = (limit != null) ? limit : 20;
        int perSourceLimit = Math.max(searchLimit / 3, 5);

        // Search pathways from KEGG and Reactome
        List<Pathway> pathways = new ArrayList<>();
        pathways.addAll(keggService.searchPathways(query, perSourceLimit));
        pathways.addAll(reactomeService.searchPathways(query, perSourceLimit));

        // Search genes from KEGG
        List<Gene> genes = keggService.searchGenes(query, "hsa", perSourceLimit);

        // Search proteins from UniProt
        List<Protein> proteins = uniProtService.searchProteins(query, perSourceLimit);

        return new SearchResult(pathways, genes, proteins);
    }

    /**
     * Get a specific pathway by ID (supports KEGG and Reactome IDs).
     */
    @QueryMapping
    public Pathway pathway(@Argument String id) {
        // Try KEGG first
        Pathway pathway = keggService.getPathway(id);
        if (pathway != null) return pathway;

        // Try Reactome
        return reactomeService.getPathway(id);
    }

    /**
     * Get pathways by organism code (e.g., "hsa" for human).
     */
    @QueryMapping
    public List<Pathway> pathwaysByOrganism(@Argument String organism, @Argument Integer limit) {
        int searchLimit = (limit != null) ? limit : 20;
        List<Pathway> pathways = new ArrayList<>();
        pathways.addAll(keggService.getPathwaysByOrganism(organism, searchLimit / 2));
        pathways.addAll(reactomeService.getPathwaysBySpecies(organism, searchLimit / 2));
        return pathways;
    }

    /**
     * Get a specific gene by its KEGG gene ID.
     */
    @QueryMapping
    public Gene gene(@Argument String id) {
        List<Gene> genes = keggService.searchGenes(id, null, 1);
        return genes.isEmpty() ? null : genes.get(0);
    }

    /**
     * Get genes associated with a specific pathway.
     */
    @QueryMapping
    public List<Gene> genesByPathway(@Argument String pathwayId) {
        // Try KEGG
        List<Gene> genes = keggService.getGenesByPathway(pathwayId);
        if (!genes.isEmpty()) return genes;

        // Try Reactome
        return reactomeService.getParticipants(pathwayId);
    }

    /**
     * Get a protein by its UniProt accession.
     */
    @QueryMapping
    public Protein protein(@Argument String accession) {
        return uniProtService.getProtein(accession);
    }

    /**
     * Get proteins associated with a gene name.
     */
    @QueryMapping
    public List<Protein> proteinsByGene(@Argument String geneName) {
        return uniProtService.getProteinsByGeneName(geneName);
    }

    /**
     * Build an interaction network for visualization.
     */
    @QueryMapping
    public InteractionNetwork interactionNetwork(@Argument String entityId, @Argument Integer depth) {
        int networkDepth = (depth != null) ? depth : 1;
        return networkService.buildNetwork(entityId, networkDepth);
    }
}
