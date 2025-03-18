package cn.woyioii.controller;

import cn.woyioii.dao.impl.RoadDaoImpl;
import cn.woyioii.dao.impl.VillageDaoImpl;
import cn.woyioii.handler.UIEventHandler;
import cn.woyioii.handler.UIEventHandler.UIEventListener;
import cn.woyioii.model.Road;
import cn.woyioii.model.Village;
import cn.woyioii.render.MapRenderer;
import cn.woyioii.service.RoadService;
import cn.woyioii.service.VillageService;  // 确保这个import存在
import cn.woyioii.service.impl.RoadServiceImpl;
import cn.woyioii.service.impl.VillageServiceImpl;
import cn.woyioii.util.AlertUtils;
import cn.woyioii.util.MapCalculator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class MainController implements UIEventListener {
    // 添加默认数据文件路径常量
    private static final String DEFAULT_DATA_FILE = "data/villages.json";
    private static final int VILLAGE_SELECT_THRESHOLD = 20; // 村庄选择的像素阈值

    // FXML 组件
    @FXML private TableView<Village> villageTable;
    @FXML private TableView<Road> roadTable;
    @FXML private Canvas mapCanvas;
    @FXML private Label statusLabel;
    @FXML private Label coordinatesLabel;

    // 侧边栏按钮
    @FXML private Button villageTabButton;
    @FXML private Button roadTabButton;
    @FXML private Button analysisTabButton;
    @FXML private Button routeTabButton;

    // 侧边栏面板
    @FXML private VBox villageOperationsPanel;
    @FXML private VBox roadOperationsPanel;
    @FXML private VBox analysisOperationsPanel;
    @FXML private VBox routeOperationsPanel;

    // 搜索框
    @FXML private ComboBox<Village> startVillageCombo;
    @FXML private TextField villageSearchField;
    @FXML private TextField roadSearchField;

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
        // 默认显示村庄面板
        switchToVillageTab();
    }

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

    //
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
    public void onOpen() {
        handleFileOperation("打开文件", () -> {
            fileManager.loadData(currentFile, villageService, roadService);
            calculateRoadLengths();
            return "已加载文件";
        });
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
            calculateRoadLengths();
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

    // Village operations
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
    public void handleSearchVillage() {
        String searchTerm = villageSearchField.getText().trim().toLowerCase();
        if (searchTerm.isEmpty()) {
            refreshUI();
            return;
        }

        List<Village> filteredVillages = villageService.getAllVillages().stream()
                .filter(v -> v.getName().toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());

        villageTable.getItems().setAll(filteredVillages);
    }

    // Road operations
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
    public void handleSearchRoad() {
        String searchTerm = roadSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            refreshUI();
            return;
        }

        // Assuming search can be by name or village ID
        List<Road> filteredRoads = roadService.getAllRoads().stream()
                .filter(road -> {
                    Village start = villageService.getVillageById(road.getStartId());
                    Village end = villageService.getVillageById(road.getEndId());
                    return road.getName().contains(searchTerm) ||
                            (start != null && start.getName().contains(searchTerm)) ||
                            (end != null && end.getName().contains(searchTerm));
                })
                .collect(Collectors.toList());

        roadTable.getItems().setAll(filteredRoads);
    }

    // Tab switching
    @FXML
    public void switchToVillageTab() {
        villageOperationsPanel.setVisible(true);
        roadOperationsPanel.setVisible(false);
        analysisOperationsPanel.setVisible(false);
        routeOperationsPanel.setVisible(false);

        highlightButton(villageTabButton);
    }

    @FXML
    public void switchToRoadTab() {
        villageOperationsPanel.setVisible(false);
        roadOperationsPanel.setVisible(true);
        analysisOperationsPanel.setVisible(false);
        routeOperationsPanel.setVisible(false);

        highlightButton(roadTabButton);
    }

    @FXML
    public void switchToAnalysisTab() {
        villageOperationsPanel.setVisible(false);
        roadOperationsPanel.setVisible(false);
        analysisOperationsPanel.setVisible(true);
        routeOperationsPanel.setVisible(false);

        highlightButton(analysisTabButton);
    }

    @FXML
    public void switchToRouteTab() {
        villageOperationsPanel.setVisible(false);
        roadOperationsPanel.setVisible(false);
        analysisOperationsPanel.setVisible(false);
        routeOperationsPanel.setVisible(true);

        highlightButton(routeTabButton);
    }

    private void highlightButton(Button activeButton) {
        villageTabButton.getStyleClass().remove("active-tab");
        roadTabButton.getStyleClass().remove("active-tab");
        analysisTabButton.getStyleClass().remove("active-tab");
        routeTabButton.getStyleClass().remove("active-tab");

        activeButton.getStyleClass().add("active-tab");
    }

    // Analysis operations
    @FXML
    public void checkConnectivity() {
        // Implementation of connectivity check
        updateStatus("正在检查连通性...");

        // Example implementation placeholder
        boolean isConnected = true; // This would come from actual analysis

        if (isConnected) {
            AlertUtils.showInformation("连通性检查", "图中所有村庄均连通");
        } else {
            AlertUtils.showWarning("连通性检查", "图中存在不连通的村庄");
        }
    }

    @FXML
    public void generateMinimumSpanningTree() {
        // Implementation for MST generation
        updateStatus("正在生成最小生成树...");

        // Example implementation placeholder
        AlertUtils.showInformation("村村通方案", "已生成村村通方案，共需修建X条道路");
    }

    @FXML
    public void showVillageStatistics() {
        // Display village statistics
        int totalVillages = villageService.getAllVillages().size();
        AlertUtils.showInformation("村庄统计", "共有" + totalVillages + "个村庄");
    }

    @FXML
    public void showRoadStatistics() {
        // Display road statistics
        int totalRoads = roadService.getAllRoads().size();
        double totalLength = roadService.getAllRoads().stream().mapToDouble(Road::getLength).sum();

        AlertUtils.showInformation("道路统计",
                "共有" + totalRoads + "条道路\n" +
                        "总长度: " + String.format("%.1f", totalLength) + "km");
    }

    // Route operations
    @FXML
    public void findShortestPath() {
        // Implementation for shortest path finding
        updateStatus("正在计算最短路径...");

        // Example implementation placeholder
        AlertUtils.showInformation("最短路径", "两点之间的最短路径已显示在地图上");
    }

    @FXML
    public void generateOptimalRoute() {
        // Implementation for TSP
        updateStatus("正在生成最优路线...");

        // Example implementation placeholder
        AlertUtils.showInformation("最优路线", "已生成经过所有村庄的最短路线");
    }

    @FXML
    public void generateOptimalRoundTrip() {
        // Implementation for TSP with return to start
        updateStatus("正在生成最优环路...");

        // Example implementation placeholder
        AlertUtils.showInformation("最优环路", "已生成经过所有村庄并返回起点的最短路线");
    }

    // Event handlers for selections and interactions
    private void handleVillageSelection(Village village) {
        if (village != null) {
            // Handle village selection (e.g., highlight on map)
            mapRenderer.highlightVillage(village);
        }
    }

    private void handleRoadSelection(Road road) {
        if (road != null) {
            // Handle road selection (e.g., highlight on map)
            Village start = villageService.getVillageById(road.getStartId());
            Village end = villageService.getVillageById(road.getEndId());
            if (start != null && end != null) {
                mapRenderer.highlightRoad(road, start, end);
            }
        }
    }

    private void handleMouseMoved(MouseEvent event) {
        // Update coordinates label
        int x = (int) event.getX();
        int y = (int) event.getY();
        coordinatesLabel.setText("坐标: " + x + "," + y);
    }

    private void handleMapClick(MouseEvent event) {
        // Handle map clicks (e.g., select closest village)
        double x = event.getX();
        double y = event.getY();

        // Find closest village and select it
        Village closest = findClosestVillage(x, y);
        if (closest != null) {
            villageTable.getSelectionModel().select(closest);
            villageTable.scrollTo(closest);
        }
    }

    private Village findClosestVillage(double x, double y) {
        // Find the village closest to the given coordinates
        Village closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Village v : villageService.getAllVillages()) {
            double distance = Math.sqrt(
                    Math.pow(v.getLocateX() - x, 2) +
                            Math.pow(v.getLocateY() - y, 2));

            if (distance < minDistance && distance < VILLAGE_SELECT_THRESHOLD) { // 20 pixel threshold
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
        // Update tables
        villageTable.getItems().setAll(villageService.getAllVillages());
        roadTable.getItems().setAll(roadService.getAllRoads());

        // Update combo boxes
        startVillageCombo.getItems().setAll(villageService.getAllVillages());

        // Redraw map
        mapRenderer.redraw(villageService.getAllVillages(), roadService.getAllRoads(), villageService);
    }

    /**
     * 更新界面状态，确保在JavaFX应用程序线程中执行
     */
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

    // 设置
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

    /**
     * 验证服务是否正确初始化
     */
    private void validateServices() {
        if (villageService == null || roadService == null) {
            throw new IllegalStateException("服务未初始化");
        }
    }

    /**
     * 加载初始数据
     */
    private void loadInitialData() {
        currentFile = new File(DEFAULT_DATA_FILE);
        if (currentFile.exists()) {
            if (loadExistingData()) {
                calculateRoadLengths();
                refreshUI();
                updateStatus("初始数据加载完成");
            }
        } else {
            createNewData();
        }
    }

    /**
     * 加载已存在的数据文件
     */
    private boolean loadExistingData() {
        return fileManager.loadData(currentFile, villageService, roadService);
    }

    /**
     * 创建新的数据文件
     */
    private void createNewData() {
        fileManager.createNewData(villageService, roadService);
        updateStatus("未找到默认数据文件，已创建新数据");
    }

    /**
     * 处理初始化错误
     */
    private void handleInitializationError(Exception e) {
        log.error("初始化界面数据失败", e);
        AlertUtils.showException("初始化失败", "无法加载初始数据", e);
    }

    // 添加新方法用于计算道路长度
    private void calculateRoadLengths() {
        List<Road> roads = roadService.getAllRoads();
        for (Road road : roads) {
            Village start = villageService.getVillageById(road.getStartId());
            Village end = villageService.getVillageById(road.getEndId());
            if (start != null && end != null) {
                double length = MapCalculator.calculateDistance(start, end);
                road.setLength(length);
            }
        }
    }

    // 添加新的私有方法来处理文件操作
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
        }
        return true;
    }

    // 文件操作接口
    @FunctionalInterface
    private interface FileOperation {
        String execute() throws Exception;
    }

    // 实现UIEventListener接口
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