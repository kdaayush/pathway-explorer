package com.pathwayexplorer.service;

import com.pathwayexplorer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for interacting with the UniProt REST API.
 * Documentation: https://www.uniprot.org/help/api
 */
@Service
public class UniProtService {

    private static final Logger log = LoggerFactory.getLogger(UniProtService.class);
    private static final String UNIPROT_BASE_URL = "https://rest.uniprot.org/uniprotkb";
    private final RestTemplate restTemplate;

    // Cache to prevent repeated upstream queries and handle rate limits
    private final Map<String, Protein> proteinCache = new ConcurrentHashMap<>();
    private final Map<String, List<Protein>> searchCache = new ConcurrentHashMap<>();

    public UniProtService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Search UniProt for proteins matching a query.
     */
    @SuppressWarnings("unchecked")
    public List<Protein> searchProteins(String query, int limit) {
        String cacheKey = (query != null ? query.trim().toLowerCase() : "") + "_" + limit;
        if (searchCache.containsKey(cacheKey)) {
            return searchCache.get(cacheKey);
        }

        List<Protein> proteins = new ArrayList<>();
        try {
            // UniProt query syntax: sanitize user query
            String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : "human";
            String url = UNIPROT_BASE_URL + "/search?query=" + cleanQuery
                    + "&format=json&size=" + limit
                    + "&fields=accession,protein_name,gene_names,organism_name,length,mass,sequence,cc_function,cc_subcellular_location,keyword,xref_pdb,xref_ensembl,xref_refseq";

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                for (Map<String, Object> result : results) {
                    Protein protein = parseUniProtEntry(result);
                    if (protein != null) {
                        proteins.add(protein);
                        proteinCache.put(protein.getAccession(), protein);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("UniProt protein search failed for query '{}': {}", query, e.getMessage());
        }

        if (!proteins.isEmpty()) {
            searchCache.put(cacheKey, proteins);
        }
        return proteins;
    }

    // Gene to UniProt accession dictionary for rapid lookup
    private static final Map<String, String> GENE_TO_ACCESSION = Map.ofEntries(
            Map.entry("TP53", "P04637"),
            Map.entry("MDM2", "Q00987"),
            Map.entry("CDKN1A", "P38936"),
            Map.entry("BAX", "Q07812"),
            Map.entry("PTEN", "P60484"),
            Map.entry("ATM", "Q13315"),
            Map.entry("BRCA1", "P38398"),
            Map.entry("BRCA2", "P51587"),
            Map.entry("RAD51", "Q06609"),
            Map.entry("INS", "P01308"),
            Map.entry("INSR", "P06213"),
            Map.entry("IRS1", "P35568"),
            Map.entry("AKT1", "P31749"),
            Map.entry("CASP3", "P42574"),
            Map.entry("BCL2", "P10415"),
            Map.entry("CASP8", "Q14790"),
            Map.entry("CASP9", "P55211"),
            Map.entry("CYCS", "P99999"),
            Map.entry("GSK3B", "P49841"),
            Map.entry("EGFR", "P00533")
    );

    /**
     * Get a protein by its UniProt accession number or gene symbol.
     */
    @SuppressWarnings("unchecked")
    public Protein getProtein(String accession) {
        if (accession == null || accession.trim().isEmpty()) return null;
        String queryTerm = accession.trim().toUpperCase();

        // Resolve gene symbol to accession if applicable
        String cleanAccession = GENE_TO_ACCESSION.getOrDefault(queryTerm, queryTerm);

        if (proteinCache.containsKey(cleanAccession)) {
            Protein cached = proteinCache.get(cleanAccession);
            if (cached.getSequence() != null && !cached.getSequence().isEmpty()) {
                return cached;
            }
        }

        // Try direct UniProt accession endpoint
        try {
            String url = UNIPROT_BASE_URL + "/" + cleanAccession + ".json";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                Protein protein = parseUniProtEntry(response);
                if (protein != null) {
                    proteinCache.put(cleanAccession, protein);
                    proteinCache.put(queryTerm, protein);
                    return protein;
                }
            }
        } catch (Exception e) {
            log.warn("UniProt direct protein fetch failed for '{}': {}", cleanAccession, e.getMessage());
        }

        // Search fallback by gene symbol or identifier
        try {
            String searchUrl = UNIPROT_BASE_URL + "/search?query=gene_exact:" + cleanAccession + "+AND+organism_id:9606+AND+reviewed:true&format=json&size=1";
            Map<String, Object> searchResponse = restTemplate.getForObject(searchUrl, Map.class);
            if (searchResponse != null && searchResponse.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("results");
                if (results != null && !results.isEmpty()) {
                    Protein protein = parseUniProtEntry(results.get(0));
                    if (protein != null) {
                        proteinCache.put(protein.getAccession(), protein);
                        proteinCache.put(cleanAccession, protein);
                        proteinCache.put(queryTerm, protein);
                        return protein;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("UniProt gene search fallback failed for '{}': {}", cleanAccession, e.getMessage());
        }

        // Return from cache if available as fallback
        return proteinCache.get(cleanAccession);
    }

    /**
     * Search for proteins associated with a specific gene name.
     */
    public List<Protein> getProteinsByGeneName(String geneName) {
        return searchProteins("gene:" + geneName, 10);
    }

    /**
     * Parse a UniProt JSON entry into a Protein model with comprehensive annotations.
     */
    @SuppressWarnings("unchecked")
    private Protein parseUniProtEntry(Map<String, Object> entry) {
        try {
            String accession = (String) entry.getOrDefault("primaryAccession", "");
            if (accession.isEmpty()) return null;

            // Extract protein name and recommended full name
            String proteinName = "";
            String fullName = "";
            Map<String, Object> protDesc = (Map<String, Object>) entry.get("proteinDescription");
            if (protDesc != null) {
                Map<String, Object> recName = (Map<String, Object>) protDesc.get("recommendedName");
                if (recName != null) {
                    Map<String, Object> fn = (Map<String, Object>) recName.get("fullName");
                    if (fn != null) {
                        fullName = (String) fn.getOrDefault("value", "");
                        proteinName = fullName;
                    }
                }
                if (proteinName.isEmpty()) {
                    List<Map<String, Object>> subNames = (List<Map<String, Object>>) protDesc.get("submissionNames");
                    if (subNames != null && !subNames.isEmpty()) {
                        Map<String, Object> fn = (Map<String, Object>) subNames.get(0).get("fullName");
                        if (fn != null) {
                            proteinName = (String) fn.getOrDefault("value", accession);
                            fullName = proteinName;
                        }
                    }
                }
            }
            if (proteinName.isEmpty()) proteinName = accession;

            // Extract organism
            String organism = "";
            Map<String, Object> org = (Map<String, Object>) entry.get("organism");
            if (org != null) {
                organism = (String) org.getOrDefault("scientificName", "");
            }

            // Extract all gene names
            List<String> geneNames = new ArrayList<>();
            String primaryGene = "";
            List<Map<String, Object>> genes = (List<Map<String, Object>>) entry.get("genes");
            if (genes != null && !genes.isEmpty()) {
                for (Map<String, Object> g : genes) {
                    Map<String, Object> gnObj = (Map<String, Object>) g.get("geneName");
                    if (gnObj != null) {
                        String gName = (String) gnObj.getOrDefault("value", "");
                        if (!gName.isEmpty()) {
                            if (primaryGene.isEmpty()) primaryGene = gName;
                            if (!geneNames.contains(gName)) geneNames.add(gName);
                        }
                    }
                    List<Map<String, Object>> syns = (List<Map<String, Object>>) g.get("synonyms");
                    if (syns != null) {
                        for (Map<String, Object> syn : syns) {
                            String sVal = (String) syn.getOrDefault("value", "");
                            if (!sVal.isEmpty() && !geneNames.contains(sVal)) {
                                geneNames.add(sVal);
                            }
                        }
                    }
                }
            }

            // Extract sequence, length, and molecular mass
            String sequenceVal = "";
            Integer length = null;
            Integer molecularMass = null;
            Map<String, Object> seqObj = (Map<String, Object>) entry.get("sequence");
            if (seqObj != null) {
                sequenceVal = (String) seqObj.getOrDefault("value", "");
                Object len = seqObj.get("length");
                if (len instanceof Number) length = ((Number) len).intValue();
                Object mass = seqObj.get("molWeight");
                if (mass instanceof Number) molecularMass = ((Number) mass).intValue();
            }

            // Extract functional annotations and biological role
            StringBuilder functionBuilder = new StringBuilder();
            List<String> locations = new ArrayList<>();
            List<Map<String, Object>> comments = (List<Map<String, Object>>) entry.get("comments");
            if (comments != null) {
                for (Map<String, Object> comment : comments) {
                    String commentType = (String) comment.getOrDefault("commentType", "");
                    if ("FUNCTION".equalsIgnoreCase(commentType)) {
                        List<Map<String, Object>> texts = (List<Map<String, Object>>) comment.get("texts");
                        if (texts != null) {
                            for (Map<String, Object> t : texts) {
                                String val = (String) t.getOrDefault("value", "");
                                if (!val.isEmpty()) {
                                    if (functionBuilder.length() > 0) functionBuilder.append(" ");
                                    functionBuilder.append(val);
                                }
                            }
                        }
                    } else if ("SUBCELLULAR LOCATION".equalsIgnoreCase(commentType)) {
                        List<Map<String, Object>> subLocs = (List<Map<String, Object>>) comment.get("subcellularLocations");
                        if (subLocs != null) {
                            for (Map<String, Object> sl : subLocs) {
                                Map<String, Object> locMap = (Map<String, Object>) sl.get("location");
                                if (locMap != null) {
                                    String locVal = (String) locMap.getOrDefault("value", "");
                                    if (!locVal.isEmpty() && !locations.contains(locVal)) {
                                        locations.add(locVal);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Extract keywords
            List<String> keywords = new ArrayList<>();
            List<Map<String, Object>> kwList = (List<Map<String, Object>>) entry.get("keywords");
            if (kwList != null) {
                for (Map<String, Object> kw : kwList) {
                    String kwName = (String) kw.getOrDefault("name", "");
                    if (!kwName.isEmpty() && !keywords.contains(kwName)) {
                        keywords.add(kwName);
                    }
                }
            }

            // Extract structural domains and feature annotations
            List<String> domains = new ArrayList<>();
            List<Map<String, Object>> features = (List<Map<String, Object>>) entry.get("features");
            if (features != null) {
                for (Map<String, Object> feature : features) {
                    String fType = (String) feature.getOrDefault("type", "");
                    String fDesc = (String) feature.getOrDefault("description", "");
                    if ("Domain".equalsIgnoreCase(fType) || "Region".equalsIgnoreCase(fType) || "Motif".equalsIgnoreCase(fType)) {
                        if (!fDesc.isEmpty() && !domains.contains(fDesc)) {
                            domains.add(fDesc);
                        }
                    }
                }
            }

            // Extract cross-references (PDB, RefSeq, Ensembl, GeneID, AlphaFoldDB)
            List<CrossReference> crossReferences = new ArrayList<>();
            List<Map<String, Object>> xRefs = (List<Map<String, Object>>) entry.get("uniProtKBCrossReferences");
            if (xRefs != null) {
                for (Map<String, Object> xr : xRefs) {
                    String db = (String) xr.getOrDefault("database", "");
                    String xid = (String) xr.getOrDefault("id", "");
                    if (!db.isEmpty() && !xid.isEmpty()) {
                        String url = buildCrossReferenceUrl(db, xid);
                        crossReferences.add(new CrossReference(db, xid, null, url));
                    }
                }
            }

            Protein protein = new Protein(accession, accession, proteinName, organism);
            protein.setFullName(fullName);
            protein.setSequence(sequenceVal);
            protein.setLength(length);
            protein.setMolecularMass(molecularMass);
            protein.setFunction(functionBuilder.toString());
            protein.setGeneNames(geneNames);
            protein.setSubcellularLocations(locations);
            protein.setKeywords(keywords);
            protein.setDomains(domains);
            protein.setCrossReferences(crossReferences);
            protein.setEntryUrl("https://www.uniprot.org/uniprotkb/" + accession + "/entry");
            protein.setSource(DataSource.UNIPROT);

            if (!primaryGene.isEmpty()) {
                Gene g = new Gene(primaryGene, primaryGene, null, organism);
                g.setSymbol(primaryGene);
                protein.setGene(g);
            }

            return protein;
        } catch (Exception e) {
            log.warn("Failed to parse UniProt entry: {}", e.getMessage());
            return null;
        }
    }

    private String buildCrossReferenceUrl(String database, String id) {
        if ("PDB".equalsIgnoreCase(database)) return "https://www.rcsb.org/structure/" + id;
        if ("Ensembl".equalsIgnoreCase(database)) return "https://www.ensembl.org/id/" + id;
        if ("RefSeq".equalsIgnoreCase(database)) return "https://www.ncbi.nlm.nih.gov/protein/" + id;
        if ("GeneID".equalsIgnoreCase(database)) return "https://www.ncbi.nlm.nih.gov/gene/" + id;
        if ("AlphaFoldDB".equalsIgnoreCase(database)) return "https://alphafold.ebi.ac.uk/entry/" + id;
        if ("GO".equalsIgnoreCase(database)) return "https://www.ebi.ac.uk/QuickGO/term/" + id;
        if ("KEGG".equalsIgnoreCase(database)) return "https://www.kegg.jp/entry/" + id;
        return null;
    }
}
