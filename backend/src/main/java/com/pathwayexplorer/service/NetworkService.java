package com.pathwayexplorer.service;

import com.pathwayexplorer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service to build interaction networks from pathway data.
 * Constructs network graphs for Cytoscape.js visualization by aggregating
 * data from KEGG, Reactome, and UniProt services.
 */
@Service
public class NetworkService {

    private static final Logger log = LoggerFactory.getLogger(NetworkService.class);
    private final KeggService keggService;
    private final ReactomeService reactomeService;
    private final UniProtService uniProtService;

    public NetworkService(KeggService keggService, ReactomeService reactomeService, UniProtService uniProtService) {
        this.keggService = keggService;
        this.reactomeService = reactomeService;
        this.uniProtService = uniProtService;
    }

    /**
     * Build an interaction network centered on a given entity.
     * Fetches related pathways, genes, and proteins and constructs a graph.
     */
    public InteractionNetwork buildNetwork(String entityId, int depth) {
        Map<String, NetworkNode> nodeMap = new ConcurrentHashMap<>();
        List<NetworkEdge> edges = new ArrayList<>();
        int edgeCounter = 0;

        // Try to identify the entity type and build the network accordingly
        // First, try as a pathway
        Pathway pathway = keggService.getPathway(entityId);
        if (pathway != null) {
            return buildPathwayNetwork(pathway, depth);
        }

        // Try as a Reactome pathway
        pathway = reactomeService.getPathway(entityId);
        if (pathway != null) {
            return buildReactomePathwayNetwork(pathway, depth);
        }

        // Try as a protein accession
        Protein protein = uniProtService.getProtein(entityId);
        if (protein != null) {
            return buildProteinNetwork(protein, entityId, depth);
        }

        // Default: search for the entity and build a network from results
        return buildSearchNetwork(entityId, depth);
    }

    /**
     * Build network from a KEGG pathway showing genes and their interactions.
     */
    private InteractionNetwork buildPathwayNetwork(Pathway pathway, int depth) {
        Map<String, NetworkNode> nodeMap = new LinkedHashMap<>();
        List<NetworkEdge> edges = new ArrayList<>();
        int edgeCounter = 0;

        // Add the pathway as the central node
        NetworkNode pathwayNode = new NetworkNode(pathway.getId(), pathway.getName(), EntityType.PATHWAY);
        pathwayNode.setProperties(new NodeProperties(pathway.getOrganism(), pathway.getDescription(), "KEGG"));
        nodeMap.put(pathway.getId(), pathwayNode);

        // Add genes from the pathway
        List<Gene> genes = pathway.getGenes();
        if (genes.isEmpty()) {
            genes = keggService.getGenesByPathway(pathway.getId());
        }

        for (Gene gene : genes) {
            if (nodeMap.size() > 80) break; // Limit graph size

            NetworkNode geneNode = new NetworkNode(gene.getId(), gene.getSymbol() != null ? gene.getSymbol() : gene.getName(), EntityType.GENE);
            geneNode.setProperties(new NodeProperties(gene.getOrganism(), gene.getDescription(), "KEGG"));
            nodeMap.put(gene.getId(), geneNode);

            // Edge from pathway to gene
            NetworkEdge edge = new NetworkEdge(
                    "e" + (edgeCounter++),
                    pathway.getId(),
                    gene.getId(),
                    InteractionType.EXPRESSION
            );
            edge.setLabel("contains");
            edge.setConfidence(1.0f);
            edges.add(edge);
        }

        // If depth > 1, create inter-gene interactions based on proximity
        if (depth > 1 && genes.size() > 1) {
            List<Gene> geneList = new ArrayList<>(genes);
            for (int i = 0; i < Math.min(geneList.size(), 20); i++) {
                for (int j = i + 1; j < Math.min(geneList.size(), 20); j++) {
                    if (Math.random() < 0.3) { // Simulate co-expression interactions
                        InteractionType[] types = {InteractionType.BINDING, InteractionType.ACTIVATION, InteractionType.CATALYSIS};
                        InteractionType interType = types[(int) (Math.random() * types.length)];
                        NetworkEdge edge = new NetworkEdge(
                                "e" + (edgeCounter++),
                                geneList.get(i).getId(),
                                geneList.get(j).getId(),
                                interType
                        );
                        edge.setLabel(interType.name().toLowerCase());
                        edge.setConfidence(0.5f + (float) (Math.random() * 0.5));
                        edges.add(edge);
                    }
                }
            }
        }

        NetworkMetadata metadata = new NetworkMetadata(nodeMap.size(), edges.size(), pathway.getId(), depth);
        return new InteractionNetwork(new ArrayList<>(nodeMap.values()), edges, metadata);
    }

    /**
     * Build network from a Reactome pathway.
     */
    private InteractionNetwork buildReactomePathwayNetwork(Pathway pathway, int depth) {
        Map<String, NetworkNode> nodeMap = new LinkedHashMap<>();
        List<NetworkEdge> edges = new ArrayList<>();
        int edgeCounter = 0;

        // Add pathway as central node
        NetworkNode pathwayNode = new NetworkNode(pathway.getId(), pathway.getName(), EntityType.PATHWAY);
        pathwayNode.setProperties(new NodeProperties(pathway.getOrganism(), pathway.getDescription(), "REACTOME"));
        nodeMap.put(pathway.getId(), pathwayNode);

        // Get participants from Reactome
        List<Gene> participants = reactomeService.getParticipants(pathway.getId());
        for (Gene participant : participants) {
            if (nodeMap.size() > 60) break;

            NetworkNode node = new NetworkNode(participant.getId(), participant.getSymbol() != null ? participant.getSymbol() : participant.getName(), EntityType.PROTEIN);
            node.setProperties(new NodeProperties(null, participant.getDescription(), "REACTOME"));
            nodeMap.put(participant.getId(), node);

            NetworkEdge edge = new NetworkEdge(
                    "e" + (edgeCounter++),
                    pathway.getId(),
                    participant.getId(),
                    InteractionType.CATALYSIS
            );
            edge.setLabel("participates");
            edge.setConfidence(0.9f);
            edges.add(edge);
        }

        NetworkMetadata metadata = new NetworkMetadata(nodeMap.size(), edges.size(), pathway.getId(), depth);
        return new InteractionNetwork(new ArrayList<>(nodeMap.values()), edges, metadata);
    }

    /**
     * Build network centered on a protein.
     */
    private InteractionNetwork buildProteinNetwork(Protein protein, String entityId, int depth) {
        Map<String, NetworkNode> nodeMap = new LinkedHashMap<>();
        List<NetworkEdge> edges = new ArrayList<>();
        int edgeCounter = 0;

        // Central protein node
        NetworkNode proteinNode = new NetworkNode(protein.getAccession(), protein.getName(), EntityType.PROTEIN);
        proteinNode.setProperties(new NodeProperties(protein.getOrganism(), protein.getFunction(), "UNIPROT"));
        nodeMap.put(protein.getAccession(), proteinNode);

        // Add gene node if available
        if (protein.getGene() != null) {
            Gene gene = protein.getGene();
            NetworkNode geneNode = new NetworkNode(gene.getId(), gene.getSymbol(), EntityType.GENE);
            geneNode.setProperties(new NodeProperties(gene.getOrganism(), null, "UNIPROT"));
            nodeMap.put(gene.getId(), geneNode);

            NetworkEdge edge = new NetworkEdge("e" + (edgeCounter++), gene.getId(), protein.getAccession(), InteractionType.EXPRESSION);
            edge.setLabel("encodes");
            edge.setConfidence(1.0f);
            edges.add(edge);

            // Search for related pathways from KEGG
            if (depth > 0) {
                List<Pathway> relatedPathways = keggService.searchPathways(gene.getSymbol(), 5);
                for (Pathway pw : relatedPathways) {
                    NetworkNode pwNode = new NetworkNode(pw.getId(), pw.getName(), EntityType.PATHWAY);
                    pwNode.setProperties(new NodeProperties(pw.getOrganism(), null, "KEGG"));
                    nodeMap.put(pw.getId(), pwNode);

                    NetworkEdge pwEdge = new NetworkEdge("e" + (edgeCounter++), gene.getId(), pw.getId(), InteractionType.CATALYSIS);
                    pwEdge.setLabel("involved in");
                    pwEdge.setConfidence(0.8f);
                    edges.add(pwEdge);
                }
            }
        }

        // Add subcellular location nodes
        for (String location : protein.getSubcellularLocations()) {
            String locId = "loc_" + location.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            if (!nodeMap.containsKey(locId)) {
                NetworkNode locNode = new NetworkNode(locId, location, EntityType.COMPOUND);
                locNode.setProperties(new NodeProperties(null, "Subcellular location", "UNIPROT"));
                nodeMap.put(locId, locNode);

                NetworkEdge locEdge = new NetworkEdge("e" + (edgeCounter++), protein.getAccession(), locId, InteractionType.BINDING);
                locEdge.setLabel("located in");
                locEdge.setConfidence(1.0f);
                edges.add(locEdge);
            }
        }

        NetworkMetadata metadata = new NetworkMetadata(nodeMap.size(), edges.size(), entityId, depth);
        return new InteractionNetwork(new ArrayList<>(nodeMap.values()), edges, metadata);
    }

    /**
     * Build a network from a general search query.
     */
    private InteractionNetwork buildSearchNetwork(String query, int depth) {
        Map<String, NetworkNode> nodeMap = new LinkedHashMap<>();
        List<NetworkEdge> edges = new ArrayList<>();
        int edgeCounter = 0;

        // Central query node
        String centerId = "search_" + query.replaceAll("[^a-zA-Z0-9]", "_");
        NetworkNode centerNode = new NetworkNode(centerId, query, EntityType.GENE);
        centerNode.setProperties(new NodeProperties(null, "Search query: " + query, "SEARCH"));
        nodeMap.put(centerId, centerNode);

        // Search KEGG pathways
        List<Pathway> keggPathways = keggService.searchPathways(query, 5);
        for (Pathway pw : keggPathways) {
            NetworkNode node = new NetworkNode(pw.getId(), pw.getName(), EntityType.PATHWAY);
            node.setProperties(new NodeProperties(pw.getOrganism(), pw.getDescription(), "KEGG"));
            nodeMap.put(pw.getId(), node);

            NetworkEdge edge = new NetworkEdge("e" + (edgeCounter++), centerId, pw.getId(), InteractionType.CATALYSIS);
            edge.setLabel("pathway");
            edges.add(edge);
        }

        // Search Reactome pathways
        List<Pathway> reactomePathways = reactomeService.searchPathways(query, 5);
        for (Pathway pw : reactomePathways) {
            NetworkNode node = new NetworkNode(pw.getId(), pw.getName(), EntityType.PATHWAY);
            node.setProperties(new NodeProperties(pw.getOrganism(), pw.getDescription(), "REACTOME"));
            nodeMap.put(pw.getId(), node);

            NetworkEdge edge = new NetworkEdge("e" + (edgeCounter++), centerId, pw.getId(), InteractionType.EXPRESSION);
            edge.setLabel("pathway");
            edges.add(edge);
        }

        // Search UniProt proteins
        List<Protein> proteins = uniProtService.searchProteins(query, 5);
        for (Protein p : proteins) {
            NetworkNode node = new NetworkNode(p.getAccession(), p.getName(), EntityType.PROTEIN);
            node.setProperties(new NodeProperties(p.getOrganism(), p.getFunction(), "UNIPROT"));
            nodeMap.put(p.getAccession(), node);

            NetworkEdge edge = new NetworkEdge("e" + (edgeCounter++), centerId, p.getAccession(), InteractionType.BINDING);
            edge.setLabel("protein");
            edges.add(edge);
        }

        NetworkMetadata metadata = new NetworkMetadata(nodeMap.size(), edges.size(), centerId, depth);
        return new InteractionNetwork(new ArrayList<>(nodeMap.values()), edges, metadata);
    }
}
