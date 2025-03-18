package cn.woyioii.controller;

import cn.woyioii.service.RoadService;
import cn.woyioii.service.VillageService;
import cn.woyioii.util.AlertUtils;
import javafx.stage.Window;
import cn.woyioii.handler.ErrorHandler;
import lombok.extern.slf4j.Slf4j;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.Optional;
import java.io.IOException;

@Slf4j
public class FileController {
    // 文件选择器
    private final FileChooser fileChooser;

    // 文件后缀常量
    private static final String VILLAGE_SUFFIX = "-villages.json";
    private static final String ROAD_SUFFIX = "-roads.json";

    public FileController() {
        this.fileChooser = new FileChooser();
        // 设置文件过滤器
        this.fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON 文件", "*.json"),
            new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
    }

    /**
     * 保存所有数据到文件
     * @param villageService 村庄服务
     * @param roadService 道路服务
     */
    public static void saveAllData(VillageService villageService, RoadService roadService) {
        ErrorHandler.safeExecute("保存数据", () -> {
            villageService.saveVillages();
            roadService.validateRoadReferences(villageService); // 保存前验证引用
            roadService.saveRoads();
            log.info("所有数据已成功保存");
        });
    }
    
    // 创建新的空白数据
    public void createNewData(VillageService villageService, RoadService roadService) {
        try {
            // 清空内存中的数据
            villageService.createNewVillages();
            roadService.createNewRoads();
            log.info("已创建新的空白数据");
        } catch (Exception e) {
            log.error("创建新数据失败", e);
            AlertUtils.showException("创建失败", "无法创建新的空白数据", e);
        }
    }

    // 打开文件对话框
    public Optional<File> showOpenDialog(Window window) {
        fileChooser.setTitle("打开村庄数据文件");
        File file = fileChooser.showOpenDialog(window);
        return Optional.ofNullable(file);
    }

    // 保存文件对话框
    public Optional<File> showSaveDialog(Window window) {
        fileChooser.setTitle("保存村庄数据文件");
        File file = fileChooser.showSaveDialog(window);
        return Optional.ofNullable(file);
    }

    /**
     * 加载数据文件
     * @param baseFile 基础文件
     * @param villageService 村庄服务
     * @param roadService 道路服务
     * @return 是否加载成功
     */
    public boolean loadData(File baseFile, VillageService villageService, RoadService roadService) {
        try {
            String basePath = baseFile.getParent();
            String baseName = baseFile.getName().replace(".json", "");
            
            // 首先加载村庄数据
            File villageFile = findMatchingFile(basePath, baseName + "-village");
            if (villageFile == null || !villageFile.exists()) {
                villageFile = baseFile; // 如果没有专门的村庄文件，使用基础文件
            }
            
            if (!villageFile.exists()) {
                throw new IOException("找不到村庄数据文件: " + villageFile);
            }
            
            // 设置并加载村庄数据
            villageService.getVillageDao().setFilePath(villageFile.getAbsolutePath());
            villageService.reloadVillages();
            
            // 加载道路数据
            File roadFile = findMatchingFile(basePath, baseName + "-road");
            if (roadFile != null && roadFile.exists()) {
                roadService.getRoadDao().setFilePath(roadFile.getAbsolutePath());
                roadService.reloadRoads();
                validateRoadReferences(roadService, villageService);
            } else {
                log.info("未找到道路数据文件，创建空的道路集合");
                roadService.createNewRoads();
            }
            
            log.info("数据加载成功 - 村庄文件: {}, 道路文件: {}", villageFile, roadFile);
            return true;
        } catch (Exception e) {
            log.error("加载数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("加载数据失败: " + e.getMessage(), e);
        }
    }
    
    private void validateRoadReferences(RoadService roadService, VillageService villageService) {
        // 验证road数据中的村庄ID引用是否有效，无效则移除
        roadService.validateRoadReferences(villageService);
    }

    private File findMatchingFile(String basePath, String prefix) {
        // 移除已有的.json后缀
        prefix = prefix.replace(".json", "");
        
        // 尝试查找对应类型的文件
        File villageFile = new File(basePath, prefix + VILLAGE_SUFFIX);
        File roadFile = new File(basePath, prefix + ROAD_SUFFIX);
        
        if (villageFile.exists()) return villageFile;
        if (roadFile.exists()) return roadFile;
        
        // 尝试查找旧格式的文件
        File legacyFile = new File(basePath, prefix + ".json");
        if (legacyFile.exists()) return legacyFile;
        
        return null;
    }

    // 保存数据到文件
    public boolean saveData(File baseFile, VillageService villageService, RoadService roadService) {
        try {
            String basePath = baseFile.getParent();

            String baseName = baseFile.getName()
                                   .replace(".json", "")
                                   .replace("-villages", "")
                                   .replace("-roads", "");
            
            // 确保目标目录存在
            File directory = new File(basePath);
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("无法创建目标目录: " + basePath);
            }
            
            // 使用新的后缀格式创建文件
            File villageFile = new File(basePath, baseName + VILLAGE_SUFFIX);
            villageService.getVillageDao().saveVillage(
                villageService.getAllVillages(), 
                villageFile.getAbsolutePath()
            );
            
            // 保存道路数据
            File roadFile = new File(basePath, baseName + ROAD_SUFFIX);
            roadService.validateRoadReferences(villageService); // 确保引用有效
            roadService.getRoadDao().saveRoad(
                roadService.getAllRoads(), 
                roadFile.getAbsolutePath()
            );
            
            log.info("成功保存数据，村庄文件: {}, 道路文件: {}", villageFile, roadFile);
            return true;
        } catch (Exception e) {
            log.error("保存数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存数据失败: " + e.getMessage(), e);
        }
    }

    // 单独加载村庄数据
    public void loadVillageData(File file, VillageService villageService) {
        try {
            villageService.getVillageDao().setFilePath(file.getAbsolutePath());
            villageService.reloadVillages();
            log.info("村庄数据加载成功: {}", file);
        } catch (Exception e) {
            log.error("加载村庄数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("加载村庄数据失败: " + e.getMessage(), e);
        }
    }

    // 单独加载道路数据
    public void loadRoadData(File file, RoadService roadService) {
        try {
            roadService.getRoadDao().setFilePath(file.getAbsolutePath());
            roadService.reloadRoads();
            log.info("道路数据加载成功: {}", file);
        } catch (Exception e) {
            log.error("加载道路数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("加载道路数据失败: " + e.getMessage(), e);
        }
    }
}
