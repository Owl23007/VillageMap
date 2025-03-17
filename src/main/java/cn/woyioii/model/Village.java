package cn.woyioii.model;

import lombok.Data;

@Data
public class Village {
    private int id;
    private String name;
    private int locateX;
    private int locateY;
    private String description;
    
    public Village() {}
    
    public Village(int id, String name, int locateX, int locateY, String description) {
        this.id = id;
        this.name = name;
        this.locateX = locateX;
        this.locateY = locateY;
        this.description = description;
    }
}
