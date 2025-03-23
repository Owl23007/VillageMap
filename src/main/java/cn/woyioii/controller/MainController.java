package cn.woyioii.controller;

import cn.woyioii.handler.UIEventHandler;
import cn.woyioii.handler.UIEventHandler.UIEventListener;
import cn.woyioii.model.Road;
import cn.woyioii.model.Village;
import cn.woyioii.render.MapRenderer;
import cn.woyioii.service.RoadService;
import cn.woyioii.service.VillageService;
import cn.woyioii.util.AlertUtils;
import cn.woyioii.util.MapCalculator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MainController implements UIEventListener {
    // 默认数据文件路径常量
    private static final String DEFAULT_DATA_FILE = "data/default-villages.json";
    private static final int VILLAGE_SELECT_THRESHOLD = 20; // 村庄选择的像素阈值

    // FXML 组件
    @FXML private TableView<Village> villageTable;
    @FXML private TableView<Road> roadTable;
    @FXML private Canvas mapCanvas;
    @FXML private Label statusLabel;
    @FXML private Label coordinatesLabel;
    @FXML private Label distanceDisplay;

    // FXML 右侧组件
    public TextArea textAreaResult;
    public ComboBox<Village> routeStartVillageComboBox;

    @FXML private ComboBox<Village> startVillageCombo;

    @FXML private TableView<PathResult> shortestPathsTable;

    // Services服务对象
    private VillageService villageService;
    private RoadService roadService;

    // UI事件处理器
    private UIEventHandler uiEventHandler;

    // 地图渲染器
    private MapRenderer mapRenderer;

    // 文件管理器
    private final FileController fileManager = new FileController();
    private File currentFile;

    //数据修改标记
    private boolean dataModified = false;

    // 右键菜单
    private ContextMenu addRoadMenu;
    private List<int[]> newRoadsToAdd;

    /**
     * FXML初始化方法，在所有@FXML注入完成后调用
     */
    @FXML
    public void initialize() {
        // 初始化地图渲染器
        mapRenderer = new MapRenderer(mapCanvas);
        // 加载表格配置
        setupTables();
        // 配置地图事件
        setupMap();
        // 更新状态
        updateStatus("等待数据加载...");
    }

    // 设置表格
    private void setupTables() {
        // 设置表格列
        villageTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> handleVillageSelection(newSelection));

        roadTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> handleRoadSelection(newSelection));

        // 设置village双击事件
        villageTable.setRowFactory(tv -> {
            TableRow<Village> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditVillage();
                }
            });
            return row;
        });

        // 设置road双击事件
        roadTable.setRowFactory(tv -> {
            TableRow<Road> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditRoad();
                }
            });
            return row;
        });
    }

    // 设置地图
    private void setupMap() {

        mapCanvas.setOnMouseMoved(this::handleMouseMoved);
        mapCanvas.setOnMouseClicked(this::handleMapClick);

    }

    @FXML
    public void onNew() {
        // 如果已有未保存修改，则提示用户
        if (dataModified) {
            boolean shouldSave = AlertUtils.showConfirmation("保存更改", "当前数据已修改，是否保存？");
            if (shouldSave) {
                onSave();
            }
        }
        
        // 清空现有数据并创建新的空白项目
        boolean confirm = AlertUtils.showConfirmation("新建项目", "确定要创建新项目吗？这将清除当前所有数据。");
        if (confirm) {
            fileManager.createNewData(villageService, roadService);
            currentFile = null;
            dataModified = false;
            refreshUI();
            updateStatus("已创建新项目");
        }
    }

    @FXML
    public void onOpenVillage() {
        handleFileOperation("打开村庄文件", () -> {
            fileManager.loadVillageData(currentFile, villageService);
            return "已加载村庄数据";
        });
    }

    @FXML
    public void onOpenRoad() {
        handleFileOperation("打开道路文件", () -> {
            fileManager.loadRoadData(currentFile, roadService);
            calculateAllRoadLengths();
            return "已加载道路数据";
        });
    }

    @FXML
    public void onSave() {
        try {
            if (currentFile == null) {
                // 如果当前没有关联文件，显示另存为对话框
                Optional<File> fileOpt = fileManager.showSaveDialog(mapCanvas.getScene().getWindow());
                if (fileOpt.isPresent()) {
                    currentFile = fileOpt.get();
                } else {
                    return; // 用户取消保存操作
                }
            }
            
            if (fileManager.saveData(currentFile, villageService, roadService)) {
                dataModified = false;
                updateStatus("数据已保存至: " + currentFile.getName());
            }
        } catch (Exception e) {
            log.error("保存数据失败", e);
            AlertUtils.showException("保存失败", "无法保存数据", e);
        }
    }

    @FXML
    public void onSaveAs() {
        try {
            Optional<File> fileOpt = fileManager.showSaveDialog(mapCanvas.getScene().getWindow());
            if (fileOpt.isPresent()) {
                currentFile = fileOpt.get();
                if (fileManager.saveData(currentFile, villageService, roadService)) {
                    dataModified = false;
                    updateStatus("数据已保存至: " + currentFile.getName());
                }
            }
        } catch (Exception e) {
            log.error("另存为失败", e);
            AlertUtils.showException("另存为失败", "无法保存数据到新文件", e);
        }
    }

    @FXML
    public void onExit() {
        if (dataModified) {
            boolean shouldSave = AlertUtils.showConfirmation("保存更改", "数据已修改，是否在退出前保存？");
            if (shouldSave) {
                onSave();
            }
        }
        Platform.exit();
    }

    @FXML
    public void handleAddVillage() {
        uiEventHandler.handleAddVillage();
    }

    @FXML
    public void handleEditVillage() {
        uiEventHandler.handleEditVillage();
    }

    @FXML
    public void handleDeleteVillage() {
        uiEventHandler.handleDeleteVillage();
    }

    @FXML
    public void handleAddRoad() {
        uiEventHandler.handleAddRoad();
    }

    @FXML
    public void handleEditRoad() {
        uiEventHandler.handleEditRoad();
    }

    @FXML
    public void handleDeleteRoad() {
        uiEventHandler.handleDeleteRoad();
    }

    @FXML
    public void checkConnectivity() {
        updateStatus("正在检查连通性...");

        // 获取所有村庄和道路
        List<Village> villages = villageService.getAllVillages();
        List<Road> roads = roadService.getAllRoads();

        if (villages.isEmpty()) {
            AlertUtils.showWarning("连通性检查", "当前没有任何村庄");
            return;
        }

        // 构建邻接矩阵
        double[][] adjacencyMatrix = dataToAdjacencyMatrix (villages, roads);

        // 检查连通性
        List<Set<Integer>> components = MapCalculator.checkConnectivity(adjacencyMatrix);

        if (components.size() == 1) {
            String current = textAreaResult.getText();
            textAreaResult.setText(current+"\n"+"当前图中所有村庄均连通");
            AlertUtils.showInformation("连通性检查", "图中所有村庄均连通");
        } else {
            // 构建未连通组的信息
            StringBuilder message = new StringBuilder("存在以下未连通的村庄组：\n\n");
            for (int i = 0; i < components.size(); i++) {
                message.append("组 ").append(i + 1).append("：");
                for (Integer index : components.get(i)) {
                    Village v = villages.get(index);
                    message.append(v.getName()).append("、");
                }
                message.setLength(message.length() - 1); // 移除最后的顿号
                message.append("\n");
            }
            String current = textAreaResult.getText();
            textAreaResult.setText(current+"\n"+message);
            AlertUtils.showInformation("连通性检查", "图中存在未连通的村庄组", String.valueOf(message));
        }

        updateStatus("连通性检查完成");
    }

    // 辅助方法：根据村庄ID获取在列表中的索引
    private int getVillageIndex(List<Village> villages, Integer id) {
        for (int i = 0; i < villages.size(); i++) {
            if (villages.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    // 最小生成树
    @FXML
    public void generateMinimumSpanningTree() {
        updateStatus("正在生成最小生成树...");

        // 获取所有村庄和道路
        List<Village> villages = villageService.getAllVillages();
        List<Road> roads = roadService.getAllRoads();

        if (villages.isEmpty()) {
            AlertUtils.showWarning("村村通方案", "当前没有任何村庄");
            return;
        }

        // 构建邻接矩阵
        double[][] adjacencyMatrix = dataToAdjacencyMatrix(villages, roads);

        // 检查连通性
        List<Set<Integer>> components = MapCalculator.checkConnectivity(adjacencyMatrix);
        // 更新文本区域
        StringBuilder result = new StringBuilder();
        result.append("村村通方案分析结果：\n");

        if (components.size() == 1) {
            // 所有村庄均连通
            result.append("- 当前图中所有村庄均已连通\n");
            result.append("- 无需新建道路，可优化现有道路配置\n");
            AlertUtils.showInformation("村村通方案", "所有村庄已连通");
        } else {
            // 存在未连通的村庄组
            List<int[]> mstEdges = MapCalculator.addNewRoadToConnect(adjacencyMatrix, villages);
            this.newRoadsToAdd = mstEdges;
            System.out.println(mstEdges);
            mapRenderer.highlightRoads(villages, mstEdges);
            
            // 创建右键菜单
            addRoadMenu = new ContextMenu();
            for(int[] edge : mstEdges) {
                Village v1 = villages.get(edge[0]-1);
                Village v2 = villages.get(edge[1]-1);
                MenuItem item = new MenuItem(String.format("添加道路: %s -> %s", v1.getName(), v2.getName()));
                item.setOnAction(e -> handleAddNewRoad(v1, v2));
                addRoadMenu.getItems().add(item);
            }
            
            // 为地图画布添加右键菜单
            mapCanvas.setOnContextMenuRequested(e -> {
                if(addRoadMenu != null && !newRoadsToAdd.isEmpty()) {
                    addRoadMenu.show(mapCanvas, e.getScreenX(), e.getScreenY());
                }
            });
            
            result.append("- 当前图中存在 ").append(components.size()).append(" 个未连通的村庄组\n");
            result.append("- 需要新建 ").append(mstEdges.size()).append(" 条道路以连通所有村庄\n");
            result.append("- 右键点击地图可快速添加建议的道路\n");
            AlertUtils.showInformation("村村通方案", "已生成村村通方案，共需修建 " + mstEdges.size() + " 条新道路\n右键点击地图可快速添加建议的道路");
        }

        String current = textAreaResult.getText();
        textAreaResult.setText(current + "\n" + result);

        updateStatus("村村通方案生成完成");
    }

    // 处理添加新道路
    private void handleAddNewRoad(Village start, Village end) {
        try {
            // 计算道路长度
            double distance = MapCalculator.calculateDistance(start, end);
            
            // 创建新道路对象
            Road newRoad = new Road();
            newRoad.setStartId(start.getId());
            newRoad.setEndId(end.getId());
            newRoad.setLength(distance);
            newRoad.setName(start.getName() + " - " + end.getName());
            
            // 保存新道路
            roadService.addRoad(newRoad);
            
            // 从待添加列表中移除该路径
            newRoadsToAdd.removeIf(edge -> 
                (edge[0] == start.getId() && edge[1] == end.getId()) || 
                (edge[0] == end.getId() && edge[1] == start.getId())
            );
            
            // 如果所有建议的道路都已添加,移除右键菜单
            if(newRoadsToAdd.isEmpty()) {
                mapCanvas.setOnContextMenuRequested(null);
                addRoadMenu = null;
            }
            
            // 刷新UI
            refreshUI();
            markDataAsModified();
            
            // 提示用户
            updateStatus("已添加道路: " + start.getName() + " -> " + end.getName());
        } catch(Exception e) {
            log.error("添加道路失败", e);
            AlertUtils.showException("添加失败", "无法添加新道路", e);
        }
    }

    // Data 转邻接矩阵
    public double[][] dataToAdjacencyMatrix(List<Village> villages, List<Road> roads) {
        // 创建邻接矩阵
        int n = villages.size();
        double[][] adjacencyMatrix = new double[n][n];

        // 构建邻接矩阵
        for (Road road : roads) {
            int startIndex = getVillageIndex(villages, road.getStartId());
            int endIndex = getVillageIndex(villages, road.getEndId());

            if (startIndex != -1 && endIndex != -1) {
                adjacencyMatrix[startIndex][endIndex] = road.getLength();
                adjacencyMatrix[endIndex][startIndex] = road.getLength();
            }
        }
        return adjacencyMatrix;
    }

    @FXML
    private void findAllShortestPaths() {
        updateStatus("正在计算所有最短路径...");

        Village startVillage = startVillageCombo.getValue();
        if(startVillage == null) {
            AlertUtils.showWarning("参数错误", "请选择起点村庄");
            return;
        }

        List<Village> villages = villageService.getAllVillages();
        List<Road> roads = roadService.getAllRoads();

        double[][] adjacencyMatrix = dataToAdjacencyMatrix(villages, roads);
        if (!MapCalculator.validateAdjacencyMatrix(adjacencyMatrix)) {
            AlertUtils.showError("数据错误", "邻接矩阵构建失败");
            return;
        }

        int startIndex = villages.indexOf(startVillage);
        
        // 计算从起点到所有其他点的最短路径
        Map<Integer, List<Integer>> allPaths = MapCalculator.findAllPairsShortestPathsWithRoute(adjacencyMatrix, startIndex);
        double[][] distances = MapCalculator.findAllPairsShortestPaths(adjacencyMatrix);
        
        shortestPathsTable.getItems().clear();

        // 设置表格行点击事件
        shortestPathsTable.setRowFactory(tv -> {
            TableRow<PathResult> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    PathResult result = row.getItem();
                    if (result.getPathVillages() != null && result.getPathVillages().size() > 1) {
                        mapRenderer.highlightPath(result.getPathVillages());
                    }
                }
            });
            return row;
        });

        // 填充表格数据
        for (int i = 0; i < villages.size(); i++) {
            if (i != startIndex) {  // 排除起点自身
                Village targetVillage = villages.get(i);
                List<Integer> path = allPaths.get(i);
                double distance = distances[startIndex][i];
                
                if (path == null || Double.isInfinite(distance)) {
                    shortestPathsTable.getItems().add(
                        new PathResult(targetVillage.getName(), -1, "不可达", null)
                    );
                    continue;
                }
                
                // 转换索引为Village对象列表
                List<Village> pathVillages = path.stream()
                    .map(villages::get)
                    .collect(Collectors.toList());
                
                // 构建路径字符串
                StringBuilder pathStr = new StringBuilder();
                for (int j = 0; j < pathVillages.size(); j++) {
                    pathStr.append(pathVillages.get(j).getName());
                    if (j < pathVillages.size() - 1) {
                        pathStr.append(" → ");
                    }
                }
                
                double roundedDistance = Math.round(distance * 10.0) / 10.0;
                shortestPathsTable.getItems().add(
                    new PathResult(targetVillage.getName(), roundedDistance, 
                        pathStr.toString(), pathVillages)
                );
            }
        }

        updateStatus("最短路径计算完成");
    }

    // 路径结果的数据类
    @Getter
    public static class PathResult {
        private final String targetVillage;
        private final double distance;
        private final String pathInfo;  // 新增路径信息字段
        private final List<Village> pathVillages; // 新增:存储路径上的村庄
        private final String distanceDisplay; // 添加distanceDisplay属性以匹配FXML绑定

        public PathResult(String targetVillage, double distance, String pathInfo, List<Village> pathVillages) {
            this.targetVillage = targetVillage;
            this.distance = distance;
            this.pathInfo = pathInfo;
            this.pathVillages = pathVillages;
            
            // 初始化distanceDisplay属性
            this.distanceDisplay = distance < 0 ? "不可达" : String.format("%.1f", distance);
        }
    }

    @FXML
    public void findOptimalRoute() {
        updateStatus("正在计算最优路径...");

        Village startVillage = routeStartVillageComboBox.getValue();
        if(startVillage == null) {
            AlertUtils.showWarning("参数错误", "请选择起点村庄");
            return;
        }

        List<Village> villages = villageService.getAllVillages();
        List<Road> roads = roadService.getAllRoads();

        // 构建邻接矩阵
        double[][] adjacencyMatrix = dataToAdjacencyMatrix(villages, roads);

        // 检查是否联通
        if (!MapCalculator.isStronglyConnected(adjacencyMatrix)) {
            AlertUtils.showWarning("村庄不连通", "当前村庄网络不是完全连通的，无法计算最优路径");
            textAreaResult.setText(textAreaResult.getText() + "\n无法计算最优路径：村庄网络不连通");
            return;
        }

        int startIndex = villages.indexOf(startVillage);
        // 调用最优路径算法
        List<Integer> optimalPath = MapCalculator.findOptimalRoute(adjacencyMatrix, startIndex);
        
        if (optimalPath.isEmpty()) {
            AlertUtils.showWarning("计算失败", "无法找到使用现有道路的有效路径");
            return;
        }

        // 验证路径是否使用了存在的道路
        boolean isValidPath = validatePath(optimalPath, roads, villages);
        if (!isValidPath) {
            AlertUtils.showWarning("路径无效", "计算出的路径包含不存在的道路");
            return;
        }

        // 计算总距离
        double totalDistance = MapCalculator.calculatePathLength(optimalPath, adjacencyMatrix);
        if (totalDistance < 0) {
            AlertUtils.showWarning("计算失败", "无法计算路径总长度");
            return;
        }

        // 转换路径为村庄对象列表
        List<Village> pathVillages = optimalPath.stream()
                .map(id -> villages.get(id - 1))
                .collect(Collectors.toList());

        // 更新路径显示
        mapRenderer.highlightPathWithRoads(pathVillages, getPathRoads(optimalPath, roads, villages));

        // 构建结果字符串
        StringBuilder result = new StringBuilder("\n最优路径计算结果：\n");
        result.append("起点: ").append(startVillage.getName()).append("\n");
        result.append("路径: ");
        for (int i = 0; i < pathVillages.size(); i++) {
            result.append(pathVillages.get(i).getName());
            if (i < pathVillages.size() - 1) {
                result.append(" → ");
            }
        }
        result.append("\n总距离: ").append(totalDistance).append("km");

        // 更新结果显示
        textAreaResult.setText(textAreaResult.getText() + result.toString());
        updateStatus("最优路径计算完成");
    }

    @FXML
    public void findOptimalRoundTrip() {
        updateStatus("正在计算最优回路...");

        Village startVillage = routeStartVillageComboBox.getValue();
        if(startVillage == null) {
            AlertUtils.showWarning("参数错误", "请选择起点村庄");
            return;
        }

        List<Village> villages = villageService.getAllVillages();
        List<Road> roads = roadService.getAllRoads();

        // 构建邻接矩阵
        double[][] adjacencyMatrix = dataToAdjacencyMatrix(villages, roads);

        // 检查是否联通
        if (!MapCalculator.isStronglyConnected(adjacencyMatrix)) {
            AlertUtils.showWarning("村庄不连通", "当前村庄网络不是完全连通的，无法计算最优回路");
            textAreaResult.setText(textAreaResult.getText() + "\n无法计算最优回路：村庄网络不连通");
            return;
        }

        int startIndex = villages.indexOf(startVillage);
        List<Integer> roundTrip = MapCalculator.findOptimalRoundTrip(adjacencyMatrix, startIndex);
        
        if (roundTrip.isEmpty()) {
            AlertUtils.showWarning("计算失败", "无法找到使用现有道路的有效回路");
            return;
        }

        // 验证回路是否使用了存在的道路
        boolean isValidPath = validatePath(roundTrip, roads, villages);
        if (!isValidPath) {
            AlertUtils.showWarning("回路无效", "计算出的回路包含不存在的道路");
            return;
        }

        // 计算总距离
        double totalDistance = MapCalculator.calculatePathLength(roundTrip, adjacencyMatrix);
        if (totalDistance < 0) {
            AlertUtils.showWarning("计算失败", "无法计算回路总长度");
            return;
        }

        // 转换回路为村庄对象列表
        List<Village> pathVillages = roundTrip.stream()
                .map(id -> villages.get(id - 1))
                .collect(Collectors.toList());

        // 更新回路显示
        mapRenderer.highlightPathWithRoads(pathVillages, getPathRoads(roundTrip, roads, villages));

        // 构建结果字符串
        StringBuilder result = new StringBuilder("\n最优回路计算结果：\n");
        result.append("起点: ").append(startVillage.getName()).append("\n");
        result.append("回路: ");
        for (int i = 0; i < pathVillages.size(); i++) {
            result.append(pathVillages.get(i).getName());
            if (i < pathVillages.size() - 1) {
                result.append(" → ");
            }
        }
        result.append(" → ").append(startVillage.getName());
        result.append("\n总距离: ").append(totalDistance).append("km");

        // 更新结果显示
        textAreaResult.setText(textAreaResult.getText() + result.toString());
        updateStatus("最优回路计算完成");
    }

    // 验证路径是否使用了存在的道路
    private boolean validatePath(List<Integer> path, List<Road> roads, List<Village> villages) {
        for (int i = 0; i < path.size() - 1; i++) {
            int currentId = villages.get(path.get(i) - 1).getId();
            int nextId = villages.get(path.get(i + 1) - 1).getId();
            boolean hasRoad = roads.stream().anyMatch(road -> 
                (road.getStartId() == currentId && road.getEndId() == nextId) ||
                (road.getStartId() == nextId && road.getEndId() == currentId)
            );
            if (!hasRoad) {
                return false;
            }
        }
        return true;
    }

    // 获取路径上的实际道路列表
    private List<Road> getPathRoads(List<Integer> path, List<Road> allRoads, List<Village> villages) {
        List<Road> pathRoads = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            int currentId = villages.get(path.get(i) - 1).getId();
            int nextId = villages.get(path.get(i + 1) - 1).getId();
            allRoads.stream()
                .filter(road -> 
                    (road.getStartId() == currentId && road.getEndId() == nextId) ||
                    (road.getStartId() == nextId && road.getEndId() == currentId))
                .findFirst()
                .ifPresent(pathRoads::add);
        }
        return pathRoads;
    }

    // 点击村庄时的逻辑
    private void handleVillageSelection(Village village) {
        if (village != null) {
            mapRenderer.highlightVillage(village);
        }
    }

    // 点击道路时的逻辑
    private void handleRoadSelection(Road road) {
        if (road != null) {
            Village start = villageService.getVillageById(road.getStartId());
            Village end = villageService.getVillageById(road.getEndId());
            if (start != null && end != null) {
                mapRenderer.highlightRoad(road);
            }
        }
    }

    // 鼠标移动时的逻辑
    private void handleMouseMoved(MouseEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        coordinatesLabel.setText("坐标: " + x + "," + y);
    }

    // 点击地图时的逻辑
    private void handleMapClick(MouseEvent event) {

        double x = event.getX();
        double y = event.getY();

        Village closest = findClosestVillage(x, y);
        if (closest != null) {
            villageTable.getSelectionModel().select(closest);
            villageTable.scrollTo(closest);
        }
    }

    // 查找最近的村庄
    private Village findClosestVillage(double x, double y) {
        Village closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Village v : villageService.getAllVillages()) {
            double distance = Math.sqrt(
                    Math.pow(v.getLocateX() - x, 2) +
                            Math.pow(v.getLocateY() - y, 2));

            if (distance < minDistance && distance < VILLAGE_SELECT_THRESHOLD) {
                minDistance = distance;
                closest = v;
            }
        }
        return closest;
    }

    public Village getSelectedVillage() {
        return villageTable.getSelectionModel().getSelectedItem();
    }

    public Road getSelectedRoad() {
        return roadTable.getSelectionModel().getSelectedItem();
    }

    public void markDataAsModified() {
        dataModified = true;
        updateStatus("数据已修改（未保存）");
    }

    public void refreshUI() {
        // 更新表格数据
        villageTable.getItems().setAll(villageService.getAllVillages());
        roadTable.getItems().setAll(roadService.getAllRoads());

        // 更新下拉框
        startVillageCombo.getItems().setAll(villageService.getAllVillages());
        routeStartVillageComboBox.getItems().setAll(villageService.getAllVillages());

        // 更新地图
        mapRenderer.redraw(villageService.getAllVillages(), roadService.getAllRoads(), villageService);
    }

    // 更新状态信息
    private void updateStatus(String message) {
        if (Platform.isFxApplicationThread()) {
            statusLabel.setText(message);
        } else {
            Platform.runLater(() -> statusLabel.setText(message));
        }
    }

    // 设置服务
    public void setServices(VillageService villageService, RoadService roadService) {
        // 直接使用传入的服务实例
        this.villageService = villageService;
        this.roadService = roadService;
        // 初始化UI事件处理器
        initializeHandlers();
        
        log.info("服务初始化完成");
    }

    // 设置舞台
    public void setStage(Stage stage) {
        stage.setOnCloseRequest(event -> {
            if (dataModified) {
                boolean shouldSave = AlertUtils.showConfirmation("保存更改", "数据已修改，是否在退出前保存？");
                if (shouldSave) {
                    onSave();
                }
            }
        });
    }

    // 初始化数据
    public void initializeData() {
        try {
            validateServices();
            loadInitialData();
            log.info("界面初始化完成");
        } catch (Exception e) {
            handleInitializationError(e);
        }
    }

   // 验证服务是否已初始化
    private void validateServices() {
        if (villageService == null || roadService == null) {
            throw new IllegalStateException("服务未初始化");
        }
    }

    // 加载初始数据
    private void loadInitialData() {
        currentFile = new File(DEFAULT_DATA_FILE);
        if (currentFile.exists()) {
            if (loadExistingData()) {
                calculateAllRoadLengths();
                refreshUI();
                updateStatus("初始数据加载完成");
            }
        } else {
            createNewData();
        }
    }

    // 加载现有数据
    private boolean loadExistingData() {
        return fileManager.loadData(currentFile, villageService, roadService);
    }

    // 创建新数据
    private void createNewData() {
        fileManager.createNewData(villageService, roadService);
        updateStatus("未找到默认数据文件，已创建新数据");
    }

    // 处理初始化错误
    private void handleInitializationError(Exception e) {
        log.error("初始化界面数据失败", e);
        AlertUtils.showException("初始化失败", "无法加载初始数据", e);
    }

    // 计算所有道路长度
    private void calculateAllRoadLengths() {
        List<Road> roads = roadService.getAllRoads();
        for (Road road : roads) {
            Village start = villageService.getVillageById(road.getStartId());
            Village end = villageService.getVillageById(road.getEndId());
            if (start != null && end != null) {
                // 计算距离
                double length = MapCalculator.calculateDistance(start, end);
                road.setLength(length);
            }
        }
    }

    // 处理文件操作
    private void handleFileOperation(String operationType, FileOperation operation) {
        if (checkUnsavedChanges()) {
            Optional<File> fileOpt = fileManager.showOpenDialog(mapCanvas.getScene().getWindow());
            if (fileOpt.isPresent()) {
                currentFile = fileOpt.get();
                try {
                    String message = operation.execute();
                    dataModified = false;
                    refreshUI();
                    updateStatus(message + ": " + currentFile.getName());
                } catch (Exception e) {
                    log.error("{}失败: {}", operationType, e.getMessage());
                    AlertUtils.showError(operationType + "失败", "无法加载文件，请检查文件格式是否正确");
                }
            }
        }
    }

    // 检查是否有未保存的更改
    private boolean checkUnsavedChanges() {
        if (!dataModified) {
            return true;
        }
        boolean shouldSave = AlertUtils.showConfirmation("保存更改", "数据已修改，是否保存？");
        if (shouldSave) {
            onSave();
            return true;
        }
        return false;
    }

    // 文件操作接口
    @FunctionalInterface
    private interface FileOperation {
        String execute() throws Exception;
    }

    @Override
    public void onDataChanged() {
        markDataAsModified();
    }

    @Override
    public void onRefreshRequired() {
        refreshUI();
    }

    // 初始化UI事件处理器
    private void initializeHandlers() {
        this.uiEventHandler = new UIEventHandler(villageService, roadService, this);
    }
}