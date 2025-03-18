package cn.woyioii.util;

import cn.woyioii.model.Village;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MapCalculator {
    
    private MapCalculator() {
        // 工具类私有构造函数
    }

    // 计算两个村庄之间的距离
    public static double calculateDistance(Village village1, Village village2) {
        int x1 = village1.getLocateX();
        int y1 = village1.getLocateY();
        int x2 = village2.getLocateX();
        int y2 = village2.getLocateY();
        
        return calculateDistance( x1,  y1,  x2,  y2);
    }

    // 计算两个坐标之间的距离
    public static double calculateDistance(int x1, int y1, int x2, int y2) {
        // 除以十并保留一位小数
        double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        return Math.round(distance / 10.0 * 10.0) / 10.0;
    }
    
    public static Village findNearestVillage(Collection<Village> villages, int x, int y) {
        return villages.stream()
                .min(Comparator.comparingDouble(v -> 
                    calculateDistance(v.getLocateX(), v.getLocateY(), x, y)))
                .orElse(null);
    }
    
    public static List<Village> sortByDistanceFrom(Collection<Village> villages, int centerX, int centerY) {
        return villages.stream()
                .sorted(Comparator.comparingDouble(v -> 
                    calculateDistance(v.getLocateX(), v.getLocateY(), centerX, centerY)))
                .collect(Collectors.toList());
    }
    
    public static int[] calculateCentroid(Collection<Village> villages) {
        if (villages.isEmpty()) {
            return new int[]{0, 0};
        }
        
        int totalX = 0;
        int totalY = 0;
        
        for (Village village : villages) {
            totalX += village.getLocateX();
            totalY += village.getLocateY();
        }
        
        int avgX = totalX / villages.size();
        int avgY = totalY / villages.size();
        
        return new int[]{avgX, avgY};
    }
}
