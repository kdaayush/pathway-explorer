package com.pathwayexplorer.service;

import com.pathwayexplorer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service for interacting with the Reactome REST API.
 * Documentation: https://reactome.org/ContentService/
 */
@Service
public class ReactomeService {

    private static final Logger log = LoggerFactory.getLogger(ReactomeService.class);
    private static final String REACTOME_BASE_URL = "https://reactome.org/ContentService";
    private final RestTemplate restTemplate;

    public ReactomeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Search Reactome for pathways matching a query.
     */
    @SuppressWarnings("unchecked")
    public List<Pathway> searchPathways(String query, int limit) {
        List<Pathway> pathways = new ArrayList<>();
        try {
            String url = REACTOME_BASE_URL + "/search/query?query=" + query + "&types=Pathway&cluster=true";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                int count = 0;
                for (Map<String, Object> resultGroup : results) {
                    if (count >= limit) break;
                    List<Map<String, Object>> entries = (List<Map<String, Object>>) resultGroup.get("entries");
                    if (entries != null) {
                        for (Map<String, Object> entry : entries) {
                            if (count >= limit) break;
                            String stId = (String) entry.get("stId");
                            String name = (String) entry.get("name");
                            String species = entry.containsKey("species") ?
                                    ((List<String>) entry.get("species")).stream().findFirst().orElse("") : "";
                            String summation = entry.containsKey("summation") ?
                                    ((List<String>) entry.get("summation")).stream().findFirst().orElse("") : "";

                            Pathway p = new Pathway(stId, name, summation, species, DataSource.REACTOME);
                            p.setUrl("https://reactome.org/content/detail/" + stId);
                            p.setCategory("Signaling");
                            pathways.add(p);
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Reactome pathway search failed for query '{}': {}", query, e.getMessage());
        }
        return pathways;
    }

    /**
     * Get detailed pathway information by its Reactome stable ID.
     */
    @SuppressWarnings("unchecked")
    public Pathway getPathway(String pathwayId) {
        try {
            String url = REACTOME_BASE_URL + "/data/query/" + pathwayId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                String name = (String) response.getOrDefault("displayName", pathwayId);
                String stId = (String) response.getOrDefault("stId", pathwayId);

                // Get summation text
                String description = "";
                if (response.containsKey("summation")) {
                    List<Map<String, Object>> summations = (List<Map<String, Object>>) response.get("summation");
                    if (!summations.isEmpty()) {
                        description = (String) summations.get(0).getOrDefault("text", "");
                    }
                }

                // Get species
                String organism = "";
                if (response.containsKey("species")) {
                    List<Map<String, Object>> speciesList = (List<Map<String, Object>>) response.get("species");
                    if (!speciesList.isEmpty()) {
                        organism = (String) speciesList.get(0).getOrDefault("displayName", "");
                    }
                }

                Pathway pathway = new Pathway(stId, name, description, organism, DataSource.REACTOME);
                pathway.setUrl("https://reactome.org/content/detail/" + stId);
                pathway.setCategory(response.containsKey("schemaClass") ? (String) response.get("schemaClass") : "Pathway");

                return pathway;
            }
        } catch (Exception e) {
            log.warn("Reactome pathway fetch failed for '{}': {}", pathwayId, e.getMessage());
        }
        return null;
    }

    /**
     * Get participants (genes/proteins) involved in a Reactome pathway.
     */
    @SuppressWarnings("unchecked")
    public List<Gene> getParticipants(String pathwayId) {
        List<Gene> genes = new ArrayList<>();
        try {
            String url = REACTOME_BASE_URL + "/data/participants/" + pathwayId;
            List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);
            if (response != null) {
                Set<String> seen = new HashSet<>();
                for (Map<String, Object> participant : response) {
                    String displayName = (String) participant.getOrDefault("displayName", "");
                    String refId = (String) participant.getOrDefault("stId", "");
                    if (!seen.contains(displayName) && !displayName.isEmpty()) {
                        seen.add(displayName);
                        Gene gene = new Gene(refId, displayName, displayName, null);
                        gene.setSymbol(displayName);
                        genes.add(gene);
                    }
                    if (genes.size() >= 50) break; // Limit to avoid overwhelming results
                }
            }
        } catch (Exception e) {
            log.warn("Reactome participants fetch failed for '{}': {}", pathwayId, e.getMessage());
        }
        return genes;
    }

    /**
     * Search Reactome for pathways in a specific species.
     */
    @SuppressWarnings("unchecked")
    public List<Pathway> getPathwaysBySpecies(String species, int limit) {
        List<Pathway> pathways = new ArrayList<>();
        try {
            // Use the top-level pathways endpoint for a species
            String speciesId = mapSpeciesNameToId(species);
            String url = REACTOME_BASE_URL + "/data/pathways/top/" + speciesId;
            List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);
            if (response != null) {
                int count = 0;
                for (Map<String, Object> entry : response) {
                    if (count >= limit) break;
                    String stId = (String) entry.getOrDefault("stId", "");
                    String name = (String) entry.getOrDefault("displayName", "");
                    Pathway p = new Pathway(stId, name, null, species, DataSource.REACTOME);
                    p.setUrl("https://reactome.org/content/detail/" + stId);
                    pathways.add(p);
                    count++;
                }
            }
        } catch (Exception e) {
            log.warn("Reactome pathways by species failed for '{}': {}", species, e.getMessage());
        }
        return pathways;
    }

    private String mapSpeciesNameToId(String species) {
        // Common species names to Reactome IDs
        Map<String, String> speciesMap = Map.of(
                "hsa", "9606",
                "Homo sapiens", "9606",
                "human", "9606",
                "mmu", "10090",
                "Mus musculus", "10090",
                "mouse", "10090"
        );
        return speciesMap.getOrDefault(species, "9606");
    }
}
