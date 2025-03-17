package cn.woyioii.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
public class Road {
    private Integer id;
    private String name;

    private Integer startId;
    private Integer endId;

    private double length;
    
    public Road(Integer startId, Integer endId, String name, double length) {
        this.startId = startId;
        this.endId = endId;
        this.name = name;
        this.length = length;
    }
}
