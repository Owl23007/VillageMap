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
 * <p>负责在Canvas上绘制村庄和道路的可视化组件。</p>
 * 
 * <h2>功能说明</h2>
 * <ul>
 *   <li>负责在JavaFX Canvas上绘制村庄节点和连接道路</li>
 *   <li>提供完整的地图可视化核心功能</li>
 *   <li>支持交互式选择和高亮显示</li>
 * </ul>
 * 
 * <h2>主要功能</h2>
 * <ol>
 *   <li>清除画布内容</li>
 *   <li>绘制村庄节点（支持选中状态）</li>
 *   <li>绘制村庄间连接道路</li>
 *   <li>支持路径高亮显示</li>
 *   <li>提供网格坐标系统</li>
 *   <li>支持道路和村庄的悬停效果</li>
 * </ol>
 * 
 * @author woyioii
 * @since 1.0
 */

public class MapRenderer {
    // Canvas相关字段
    private final Canvas canvas;
    private final GraphicsContext gc;

    // 状态相关字段
    @Setter private Village selectedVillage;    // 当前选中的村庄
    @Setter private Road selectedRoad;          // 当前选中的道路
    private List<Road> highlightedPath = new ArrayList<>();  // 高亮显示的路径

    // 颜色配置
    private final Color villageColor = Color.BLUE;           // 普通村庄颜色
    private final Color selectedVillageColor = Color.RED;    // 选中村庄颜色
    private final Color roadColor = Color.GRAY;              // 普通道路颜色
    private final Color pathColor = Color.GREEN;             // 路径高亮颜色
    private final Color hoveredRoadColor = Color.ORANGE;     // 悬停道路颜色

    // 缓存数据
    private List<Village> lastVillages = new ArrayList<>();
    private List<Road> lastRoads = new ArrayList<>();
    private VillageService lastVillageService;

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

    public void drawRoads(List<Road> roads) {
        roads.forEach(road -> {
            // 判断是否为高亮路径的一部分
            boolean isHighlighted = highlightedPath.contains(road);
            drawRoad(road, isHighlighted);
        });
    }

    /**
     * 绘制网格系统
     * 每10像素代表1公里，每100像素显示刻度
     */
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

    /**
     * 绘制单个村庄
     * 包含选中效果、发光效果和文字标注
     */
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
            gc.setFont(javafx.scene.text.Font.font(18)); // 选中时字体放大
            gc.fillText(village.getName(), x + radius + 5, y + 5);
        } else {
            gc.setFill(Color.BLACK);
            gc.setFont(javafx.scene.text.Font.font(15));
            gc.fillText(village.getName(), x + radius + 3, y + 3);
        }
    }

    /**
     * 绘制道路连接
     * 支持高亮显示和悬停效果
     */
    private void drawRoad(Road road , boolean highlighted) {
        Village start = lastVillageService.getVillageById(road.getStartId());
        Village end = lastVillageService.getVillageById(road.getEndId());

        if (start != null && end != null) {
            double x1 = start.getLocateX();
            double y1 = start.getLocateY();
            double x2 = end.getLocateX();
            double y2 = end.getLocateY();

            // 设置道路样式
            Color currentColor;
            if (road.equals(selectedRoad)) {
                currentColor = hoveredRoadColor;
                gc.setLineWidth(2.5);
            } else if ( highlighted ) {
                currentColor = pathColor;
                gc.setLineWidth(3);
            } else {
                currentColor = roadColor;
                gc.setLineWidth(1.5);
            }
            gc.setStroke(currentColor);

            // 绘制道路线条
            gc.strokeLine(x1, y1, x2, y2);

            // 计算道路中点和角度
            double midX = (x1 + x2) / 2;
            double midY = (y1 + y2) / 2;
            double angle = Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI;

            // 保存当前图形状态
            gc.save();
            
            // 绘制文本
            String roadInfo = road.getName() + " (" + road.getLength() + "km)";
            
            // 设置字体
            gc.setFont(javafx.scene.text.Font.font(12));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.setTextBaseline(javafx.geometry.VPos.CENTER);
            
            // 平移到文本位置，增加垂直偏移以避免遮挡道路
            double offsetDistance = -10;
            double perpendicular = angle + Math.PI / 2;
            
            double offsetY = offsetDistance * Math.sin(perpendicular * Math.PI / 180);
            gc.translate(midX, midY + offsetY);
            
            // 如果角度在90到270度之间，翻转文本以保证可读性
            if (angle > 90 && angle <= 270) {
                angle -= 180;
            }
            
            // 旋转画布以绘制文本
            gc.rotate(angle);
            
            // 创建文本背景
            double padding = 2;
            double textWidth = gc.getFont().getSize() * roadInfo.length() * 0.6;
            double textHeight = gc.getFont().getSize() + 2;
            
            gc.setFill(Color.rgb(255, 255, 255, 0.85));
            gc.fillRect(-textWidth/2 - padding, -textHeight/2 - padding,
                       textWidth + padding * 2, textHeight + padding * 2);
            
            // 根据路径状态设置文本颜色
            if (highlighted) {
                gc.setFill(Color.rgb(0, 120, 0));  // 深绿色文字
                gc.setFont(javafx.scene.text.Font.font(13));  // 稍微加大字号
            } else if (road.equals(selectedRoad)) {
                gc.setFill(Color.rgb(200, 100, 0));  // 深橙色文字
            } else {
                gc.setFill(Color.BLACK);
            }
            gc.fillText(roadInfo, 0, 0);
            
            // 恢复图形状态
            gc.restore();
        }
    }

    /**
     * 重绘整个地图
     * 包括网格、道路和村庄的完整绘制
     */
    public void redraw(List<Village> villages, List<Road> roads, VillageService villageService) {
        // 保存数据以供后续重绘使用
        this.lastVillages = new ArrayList<>(villages);
        this.lastRoads = new ArrayList<>(roads);
        this.lastVillageService = villageService;
        
        // 执行实际的重绘操作
        clear();
        drawGrid();  // 先绘制网格
        drawRoads(roads);
        drawVillages(villages);
    }

    public void highlightVillage(Village village) {
        // 更新选中的村庄
        this.selectedVillage = village;
        // 立即重绘地图以显示选中效果
        redraw(List.copyOf(getLastVillages()), List.copyOf(getLastRoads()), getLastVillageService());
    }

    public void highlightRoad(Road road) {
        // 更新选中的道路
        this.selectedRoad = road;
        // 立即重绘地图以显示高亮效果
        redraw(List.copyOf(getLastVillages()), List.copyOf(getLastRoads()), getLastVillageService());
    }
    
    // Getter方法
    private List<Village> getLastVillages() {
        return lastVillages;
    }
    
    private List<Road> getLastRoads() {
        return lastRoads;
    }
    
    private VillageService getLastVillageService() {
        return lastVillageService;
    }

    // 高亮一组道路
    public void highlightRoads(List<Village> villages, List<int[]> mstEdges) {
        // 高亮显示MST的所有边
        List<Road> path = new ArrayList<>();

        int index = 0;
        for (int[] edge : mstEdges) {
            int startId = edge[0];
            int endId = edge[1];
            Village start = villages.stream().filter(v -> v.getId() == startId).findFirst().orElse(null);
            Village end = villages.stream().filter(v -> v.getId() == endId).findFirst().orElse(null);
            if (start != null && end != null) {
                index++;
                String temp = "MST-" + index;
                Road road = new Road(start.getId(), end.getId(),temp);
                path.add(road);
            }
        }
       this.highlightedPath = path;
        drawRoads(path);
    }

    public void highlightPath(List<Village> pathVillages) {
        // 清除现有高亮
        redraw(lastVillages, lastRoads, lastVillageService);
        
        if (pathVillages == null || pathVillages.size() < 2) {
            return;
        }

        GraphicsContext gc = getGraphicsContext(pathVillages);
        for (Village v : pathVillages) {
            gc.fillOval(
                v.getLocateX() - 5,
                v.getLocateY() - 5,
                10,
                10
            );
        }
    }

    private GraphicsContext getGraphicsContext(List<Village> pathVillages) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.RED);
        gc.setLineWidth(3);

        // 绘制路径线段
        for (int i = 0; i < pathVillages.size() - 1; i++) {
            Village current = pathVillages.get(i);
            Village next = pathVillages.get(i + 1);

            gc.strokeLine(
                current.getLocateX(),
                current.getLocateY(),
                next.getLocateX(),
                next.getLocateY()  // 添加缺少的y坐标参数
            );
        }

        // 高亮路径上的村庄
        gc.setFill(Color.RED);
        return gc;
    }

    /**
     * 高亮显示旅行商问题的最优回路
     */
    public void highlightRoundTrip(List<Integer> roundTrip) {
        // 清除现有高亮
        redraw(lastVillages, lastRoads, lastVillageService);
        
        if (roundTrip == null || roundTrip.size() < 2) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.PURPLE);
        gc.setLineWidth(4);
        
        // 绘制路径线段
        for (int i = 0; i < roundTrip.size() - 1; i++) {
            Village current = lastVillages.get(roundTrip.get(i) - 1);
            Village next = lastVillages.get(roundTrip.get(i + 1) - 1);
            
            gc.strokeLine(
                current.getLocateX(),
                current.getLocateY(),
                next.getLocateX(),
                next.getLocateY()
            );
        }

        // 连接最后一个村庄到起点
        Village start = lastVillages.get(roundTrip.getFirst() - 1);
        Village end = lastVillages.get(roundTrip.getLast() - 1);
        gc.strokeLine(
            end.getLocateX(),
            end.getLocateY(),
            start.getLocateX(),
            start.getLocateY()  // 添加缺少的y坐标参数
        );

        // 高亮路径上的村庄
        gc.setFill(Color.PURPLE);
        for (Integer villageId : roundTrip) {
            Village v = lastVillages.get(villageId - 1);
            gc.fillOval(
                v.getLocateX() - 5,
                v.getLocateY() - 5,
                10,
                10
            );
        }
    }

    /**
     * 高亮显示实际道路路径
     */
    public void highlightPathWithRoads(List<Village> pathVillages, List<Road> pathRoads) {
        // 清除现有高亮
        redraw(lastVillages, lastRoads, lastVillageService);
        
        if (pathVillages == null || pathVillages.size() < 2 || pathRoads == null || pathRoads.isEmpty()) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // 高亮道路
        for (Road road : pathRoads) {
            Village start = lastVillageService.getVillageById(road.getStartId());
            Village end = lastVillageService.getVillageById(road.getEndId());
            
            if (start != null && end != null) {
                gc.setStroke(Color.RED);
                gc.setLineWidth(3);
                gc.strokeLine(
                    start.getLocateX(),
                    start.getLocateY(),
                    end.getLocateX(),
                    end.getLocateY()
                );
            }
        }

        // 高亮路径上的村庄
        gc.setFill(Color.RED);
        for (Village v : pathVillages) {
            gc.fillOval(
                v.getLocateX() - 5,
                v.getLocateY() - 5,
                10,
                10
            );
        }
    }
}