package cn.woyioii.render;

import cn.woyioii.model.Road;
import cn.woyioii.model.Village;
import cn.woyioii.service.VillageService;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
/**
 * <h1>地图渲染器</h1>
 * 负责在Canvas上绘制村庄和道路的可视化组件。
 *
 * <h2>核心功能：</h2>
 * <ul>
 *     <li>清除画布内容</li>
 *     <li>绘制村庄节点</li>
 *     <li>绘制连接道路</li>
 * </ul>
 *
 * <h2>依赖说明：</h2>
 * <ul>
 *     <li>需要JavaFX的Canvas组件支持</li>
 *     <li>与{@code VillageService}协同工作以获取村庄信息</li>
 *     <li>使用{@code Village}和{@code Road}模型类</li>
 * </ul>
 *
 * <h2>使用须知：</h2>
 * <ul>
 *     <li>在使用前需要初始化Canvas对象</li>
 *     <li>所有绘制操作都在JavaFX应用线程中执行</li>
 *     <li>支持动态更新和重绘</li>
 * </ul>
 *
 * @author woyioii
 * @see cn.woyioii.model.Village
 * @see cn.woyioii.model.Road
 * @see VillageService
 * @since 1.0
 */

public class MapRenderer {
    private final Canvas canvas;
    private final GraphicsContext gc;

    // 设置选中的村庄
    // 存储当前选中的村庄和高亮路径
    @Setter
    private Village selectedVillage;
    private List<Road> highlightedPath = new ArrayList<>();

    // 基本颜色配置
    private final Color villageColor = Color.BLUE;
    private final Color selectedVillageColor = Color.RED;
    private final Color selectedVillageGlowColor = Color.rgb(255, 100, 100, 0.3);
    private final Color roadColor = Color.GRAY;
    private final Color pathColor = Color.GREEN;

    public MapRenderer(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
    }

    public void clear() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void drawVillages(List<Village> villages) {
        villages.forEach(village -> {
            // 判断是否为选中的村庄，使用不同颜色绘制
            if (village.equals(selectedVillage)) {
                gc.setFill(selectedVillageColor);
            } else {
                gc.setFill(villageColor);
            }
            drawVillage(village);
        });
    }

    public void drawRoads(List<Road> roads, VillageService villageService) {
        roads.forEach(road -> {
            // 判断是否为高亮路径的一部分
            boolean isHighlighted = highlightedPath.contains(road);
            drawRoad(road, villageService, isHighlighted);
        });
    }

    // 高亮显示一条路径（一系列相连的道路）
    public void highlightPath(List<Road> path) {
        this.highlightedPath = new ArrayList<>(path);
    }

    // 清除所有高亮和选择
    public void clearSelection() {
        this.selectedVillage = null;
        this.highlightedPath.clear();
    }

    // 绘制网格
    public void drawGrid() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        // 设置网格样式
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.5);

        // 绘制垂直线（每10像素一条，代表1km）
        for (int i = 0; i <= width; i += 10) {
            if (i % 100 == 0) {
                gc.setLineWidth(1.0);
                gc.setStroke(Color.GRAY);
            } else {
                gc.setLineWidth(0.5);
                gc.setStroke(Color.LIGHTGRAY);
            }
            gc.strokeLine(i, 0, i, height);
        }

        // 绘制水平线
        for (int i = 0; i <= height; i += 10) {
            if (i % 100 == 0) {
                gc.setLineWidth(1.0);
                gc.setStroke(Color.GRAY);
            } else {
                gc.setLineWidth(0.5);
                gc.setStroke(Color.LIGHTGRAY);
            }
            gc.strokeLine(0, i, width, i);
        }

        // 标记主要网格线刻度（每100像素，代表10km）
        gc.setFill(Color.GRAY);
        gc.setFont(javafx.scene.text.Font.font(10));
        for (int i = 0; i <= width; i += 100) {
            gc.fillText(String.valueOf(i/10), i+2, 12);
        }
        for (int i = 0; i <= height; i += 100) {
            gc.fillText(String.valueOf(i/10), 2, i+12);
        }
    }

    // 绘制村庄
    private void drawVillage(Village village) {
        double x = village.getLocateX();
        double y = village.getLocateY();
        double radius = 5; // 默认半径

        if (village.equals(selectedVillage)) {
            // 选中村庄特效
            radius = 8;  // 放大效果
            
            // 绘制外发光效果
            gc.setFill(Color.rgb(255, 100, 100, 0.2));
            gc.fillOval(x - radius - 8, y - radius - 8, (radius + 8) * 2, (radius + 8) * 2);
            
            // 绘制中间光晕
            gc.setFill(Color.rgb(255, 50, 50, 0.3));
            gc.fillOval(x - radius - 4, y - radius - 4, (radius + 4) * 2, (radius + 4) * 2);
            
            // 绘制村庄主体
            gc.setFill(Color.RED);
        } else {
            // 非选中状态使用默认样式
            gc.setFill(villageColor);
        }

        // 绘制村庄主体
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        // 绘制边框
        if (village.equals(selectedVillage)) {
            // 选中状态边框效果
            gc.setStroke(Color.rgb(200, 0, 0));
            gc.setLineWidth(2);
            gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
            
            // 外圈动态效果
            gc.setStroke(Color.rgb(255, 0, 0, 0.4));
            gc.setLineWidth(1);
            gc.strokeOval(x - radius - 6, y - radius - 6, (radius + 6) * 2, (radius + 6) * 2);
        }

        // 绘制村庄名称
        if (village.equals(selectedVillage)) {
            gc.setFill(Color.RED);
            gc.setFont(javafx.scene.text.Font.font(14)); // 选中时字体放大
            gc.fillText(village.getName(), x + radius + 5, y + 5);
        } else {
            gc.setFill(Color.BLACK);
            gc.setFont(javafx.scene.text.Font.font(12));
            gc.fillText(village.getName(), x + radius + 3, y + 3);
        }
    }

    // 绘制道路
    private void drawRoad(Road road, VillageService villageService, boolean highlighted) {
        Village start = villageService.getVillageById(road.getStartId());
        Village end = villageService.getVillageById(road.getEndId());

        if (start != null && end != null) {
            double x1 = start.getLocateX();
            double y1 = start.getLocateY();
            double x2 = end.getLocateX();
            double y2 = end.getLocateY();

            // 设置高亮或普通道路颜色
            if (highlighted) {
                gc.setStroke(pathColor);
                gc.setLineWidth(3); // 高亮路径加粗显示
            } else {
                gc.setStroke(roadColor);
                gc.setLineWidth(2);
            }

            // 绘制道路线条
            gc.strokeLine(x1, y1, x2, y2);

            // 绘制道路名称和距离
            double midX = (x1 + x2) / 2;
            double midY = (y1 + y2) / 2;
            gc.setFill(Color.BLACK);
            String roadInfo = road.getName() + " (" + road.getLength() + "km)";
            gc.fillText(roadInfo, midX, midY);
        }
    }

    // 重绘整张地图（包括高亮和选择）
    public void redraw(List<Village> villages, List<Road> roads, VillageService villageService) {
        // 保存数据以供后续重绘使用
        this.lastVillages = new ArrayList<>(villages);
        this.lastRoads = new ArrayList<>(roads);
        this.lastVillageService = villageService;
        
        // 执行实际的重绘操作
        clear();
        drawGrid();  // 先绘制网格
        drawRoads(roads, villageService);
        drawVillages(villages);
    }

    public void highlightVillage(Village village) {
        // 更新选中的村庄
        this.selectedVillage = village;
        // 立即重绘地图以显示选中效果
        redraw(List.copyOf(getLastVillages()), List.copyOf(getLastRoads()), getLastVillageService());
    }
    
    // 添加字段用于保存最后一次绘制的数据
    private List<Village> lastVillages = new ArrayList<>();
    private List<Road> lastRoads = new ArrayList<>();
    private VillageService lastVillageService;
    
    // 添加获取方法
    private List<Village> getLastVillages() {
        return lastVillages;
    }
    
    private List<Road> getLastRoads() {
        return lastRoads;
    }
    
    private VillageService getLastVillageService() {
        return lastVillageService;
    }

    public void highlightRoad(Road road, Village start, Village end) {
    }
}