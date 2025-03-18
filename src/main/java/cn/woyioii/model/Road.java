package cn.woyioii.model;

import lombok.Data;

@Data
public class Road {
    private Integer id;
    private Integer startId;
    private Integer endId; 
    private String name;
    private double length;
    
    public Road() {}
    
    public Road(Integer startId, Integer endId, String name) {
        this.startId = startId;
        this.endId = endId;
        this.name = name;
    }
    
    public Road(Integer startId, Integer endId, String name, double length) {
        this.startId = startId;
        this.endId = endId;
        this.name = name;
        this.length = length;
    }
}
