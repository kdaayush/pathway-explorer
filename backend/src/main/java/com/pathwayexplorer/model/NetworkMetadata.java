package com.pathwayexplorer.model;

public class NetworkMetadata {
    private int totalNodes;
    private int totalEdges;
    private String centerEntity;
    private int depth;

    public NetworkMetadata() {}

    public NetworkMetadata(int totalNodes, int totalEdges, String centerEntity, int depth) {
        this.totalNodes = totalNodes;
        this.totalEdges = totalEdges;
        this.centerEntity = centerEntity;
        this.depth = depth;
    }

    public int getTotalNodes() { return totalNodes; }
    public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }

    public int getTotalEdges() { return totalEdges; }
    public void setTotalEdges(int totalEdges) { this.totalEdges = totalEdges; }

    public String getCenterEntity() { return centerEntity; }
    public void setCenterEntity(String centerEntity) { this.centerEntity = centerEntity; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
}
