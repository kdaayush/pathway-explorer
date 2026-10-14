package com.pathwayexplorer.model;

import java.util.List;

public class InteractionNetwork {
    private List<NetworkNode> nodes;
    private List<NetworkEdge> edges;
    private NetworkMetadata metadata;

    public InteractionNetwork() {}

    public InteractionNetwork(List<NetworkNode> nodes, List<NetworkEdge> edges, NetworkMetadata metadata) {
        this.nodes = nodes;
        this.edges = edges;
        this.metadata = metadata;
    }

    public List<NetworkNode> getNodes() { return nodes; }
    public void setNodes(List<NetworkNode> nodes) { this.nodes = nodes; }

    public List<NetworkEdge> getEdges() { return edges; }
    public void setEdges(List<NetworkEdge> edges) { this.edges = edges; }

    public NetworkMetadata getMetadata() { return metadata; }
    public void setMetadata(NetworkMetadata metadata) { this.metadata = metadata; }
}
