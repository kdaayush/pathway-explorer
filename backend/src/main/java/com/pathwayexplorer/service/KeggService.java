package com.pathwayexplorer.service;

import com.pathwayexplorer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for interacting with the KEGG (Kyoto Encyclopedia of Genes and Genomes) REST API.
 * Documentation: https://rest.kegg.jp/
 */
@Service
public class KeggService {

    private static final Logger log = LoggerFactory.getLogger(KeggService.class);
    private static final String KEGG_BASE_URL = "https://rest.kegg.jp";
    private final RestTemplate restTemplate;

    public KeggService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Search KEGG for pathways matching the given query.
     */
    public List<Pathway> searchPathways(String query, int limit) {
        List<Pathway> pathways = new ArrayList<>();
        try {
            String url = KEGG_BASE_URL + "/find/pathway/" + query;
            String response = restTemplate.getForObject(url, String.class);
            if (response != null && !response.isEmpty()) {
                String[] lines = response.split("\n");
                int count = 0;
                for (String line : lines) {
                    if (count >= limit) break;
                    String[] parts = line.split("\t", 2);
                    if (parts.length == 2) {
                        String id = parts[0].replace("map:", "").trim();
                        String name = parts[1].trim();
                        Pathway p = new Pathway(id, name, null, null, DataSource.KEGG);
                        p.setUrl("https://www.kegg.jp/pathway/" + id);
                        p.setCategory("Metabolic");
                        pathways.add(p);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("KEGG pathway search failed for query '{}': {}", query, e.getMessage());
        }
        return pathways;
    }

    /**
     * Get detailed information about a specific KEGG pathway.
     */
    public Pathway getPathway(String pathwayId) {
        try {
            String url = KEGG_BASE_URL + "/get/" + pathwayId;
            String response = restTemplate.getForObject(url, String.class);
            if (response != null && !response.isEmpty()) {
                return parseKeggPathwayEntry(pathwayId, response);
            }
        } catch (Exception e) {
            log.warn("KEGG pathway fetch failed for '{}': {}", pathwayId, e.getMessage());
        }
        return null;
    }

    /**
     * Search KEGG for genes matching the given query in a specific organism.
     */
    public List<Gene> searchGenes(String query, String organism, int limit) {
        List<Gene> genes = new ArrayList<>();
        String org = (organism != null && !organism.isEmpty()) ? organism : "hsa";
        try {
            String url = KEGG_BASE_URL + "/find/genes/" + query;
            String response = restTemplate.getForObject(url, String.class);
            if (response != null && !response.isEmpty()) {
                String[] lines = response.split("\n");
                int count = 0;
                for (String line : lines) {
                    if (count >= limit) break;
                    String[] parts = line.split("\t", 2);
                    if (parts.length == 2) {
                        String id = parts[0].trim();
                        String desc = parts[1].trim();
                        // Extract gene symbol from description
                        String symbol = id;
                        String name = desc;
                        if (desc.contains(";")) {
                            String[] descParts = desc.split(";", 2);
                            symbol = descParts[0].trim();
                            name = descParts.length > 1 ? descParts[1].trim() : desc;
                        }
                        Gene gene = new Gene(id, name, desc, org);
                        gene.setSymbol(symbol);
                        genes.add(gene);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("KEGG gene search failed for query '{}': {}", query, e.getMessage());
        }
        return genes;
    }

    /**
     * Get genes associated with a specific KEGG pathway.
     */
    public List<Gene> getGenesByPathway(String pathwayId) {
        List<Gene> genes = new ArrayList<>();
        try {
            String url = KEGG_BASE_URL + "/link/genes/" + pathwayId;
            String response = restTemplate.getForObject(url, String.class);
            if (response != null && !response.isEmpty()) {
                String[] lines = response.split("\n");
                for (String line : lines) {
                    String[] parts = line.split("\t");
                    if (parts.length == 2) {
                        String geneId = parts[1].trim();
                        Gene gene = new Gene(geneId, geneId, null, null);
                        gene.setSymbol(geneId);
                        genes.add(gene);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("KEGG genes by pathway failed for '{}': {}", pathwayId, e.getMessage());
        }
        return genes;
    }

    /**
     * Get pathways for a specific organism.
     */
    public List<Pathway> getPathwaysByOrganism(String organism, int limit) {
        List<Pathway> pathways = new ArrayList<>();
        try {
            String url = KEGG_BASE_URL + "/list/pathway/" + organism;
            String response = restTemplate.getForObject(url, String.class);
            if (response != null && !response.isEmpty()) {
                String[] lines = response.split("\n");
                int count = 0;
                for (String line : lines) {
                    if (count >= limit) break;
                    String[] parts = line.split("\t", 2);
                    if (parts.length == 2) {
                        String id = parts[0].replace("path:", "").trim();
                        String name = parts[1].trim();
                        // Remove organism suffix like " - Homo sapiens (human)"
                        if (name.contains(" - ")) {
                            name = name.substring(0, name.lastIndexOf(" - "));
                        }
                        Pathway p = new Pathway(id, name, null, organism, DataSource.KEGG);
                        p.setUrl("https://www.kegg.jp/pathway/" + id);
                        pathways.add(p);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("KEGG pathways by organism failed for '{}': {}", organism, e.getMessage());
        }
        return pathways;
    }

    /**
     * Parse a KEGG flat-file format entry for a pathway.
     */
    private Pathway parseKeggPathwayEntry(String pathwayId, String entry) {
        String name = "";
        String description = "";
        String organism = "";
        String category = "";
        List<Gene> genes = new ArrayList<>();

        String currentSection = "";
        for (String line : entry.split("\n")) {
            if (line.startsWith("NAME")) {
                name = line.substring(12).trim();
            } else if (line.startsWith("DESCRIPTION")) {
                description = line.substring(12).trim();
            } else if (line.startsWith("ORGANISM")) {
                organism = line.substring(12).trim();
            } else if (line.startsWith("CLASS")) {
                category = line.substring(12).trim();
            } else if (line.startsWith("GENE")) {
                currentSection = "GENE";
                String geneData = line.substring(12).trim();
                Gene gene = parseKeggGene(geneData);
                if (gene != null) genes.add(gene);
            } else if (currentSection.equals("GENE") && line.startsWith("            ")) {
                Gene gene = parseKeggGene(line.trim());
                if (gene != null) genes.add(gene);
            } else if (!line.startsWith(" ")) {
                currentSection = "";
            }
        }

        Pathway pathway = new Pathway(pathwayId, name, description, organism, DataSource.KEGG);
        pathway.setUrl("https://www.kegg.jp/pathway/" + pathwayId);
        pathway.setImageUrl("https://rest.kegg.jp/get/" + pathwayId + "/image");
        pathway.setCategory(category);
        pathway.setGenes(genes);
        return pathway;
    }

    private Gene parseKeggGene(String geneData) {
        if (geneData == null || geneData.isEmpty()) return null;
        String[] parts = geneData.split("\\s+", 2);
        String id = parts[0];
        String desc = parts.length > 1 ? parts[1] : id;
        String symbol = id;
        if (desc.contains(";")) {
            symbol = desc.split(";")[0].trim();
        }
        Gene gene = new Gene(id, desc, desc, null);
        gene.setSymbol(symbol);
        return gene;
    }
}
