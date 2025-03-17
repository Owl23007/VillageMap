package cn.woyioii.controller;

import cn.woyioii.service.RoadService;
import cn.woyioii.service.VillageService;
import cn.woyioii.util.AlertUtils;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import cn.woyioii.model.Village;
import cn.woyioii.model.Road;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

@Slf4j
public class FileController {
    /**
     * 保存所有数据到当前文件
     * @param villageService 村庄服务
     * @param roadService 道路服务
     * @return 保存成功返回true，否则返回false
     */
    public static boolean saveAllData(VillageService villageService, RoadService roadService) {
        try {
            // 保存村庄和道路数据
            villageService.saveVillages();
            roadService.saveRoads();
            log.info("所有数据保存成功");
            return true;
        } catch (Exception e) {
            log.error("保存数据失败", e);
            AlertUtils.showException("保存失败", "无法保存数据", e);
            return false;
        }
    }
    
    /**
     * 打开文件对话框选择数据文件并加载
     * @param stage 父窗口
     * @param villageService 村庄服务
     * @param roadService 道路服务
     * @return 加载成功返回true，否则返回false
     */
    public static boolean openDataFiles(Stage stage, VillageService villageService, RoadService roadService) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择村庄数据文件");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON文件", "*.json")
            );
            
            // 选择村庄文件
            File villageFile = fileChooser.showOpenDialog(stage);
            if (villageFile != null) {
                // 更新村庄DAO文件路径并重新加载
                villageService.getVillageDao().setFilePath(villageFile.getAbsolutePath());
                villageService.reloadVillages();
                
                // 选择道路文件
                fileChooser.setTitle("选择道路数据文件");
                File roadFile = fileChooser.showOpenDialog(stage);
                if (roadFile != null) {
                    roadService.getRoadDao().setFilePath(roadFile.getAbsolutePath());
                    roadService.reloadRoads();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("打开数据文件失败", e);
            AlertUtils.showException("打开失败", "无法打开数据文件", e);
            return false;
        }
    }
    
    /**
     * 另存为新文件
     * @param stage 父窗口
     * @param villageService 村庄服务
     * @param roadService 道路服务
     * @return 保存成功返回true，否则返回false
     */
    public static boolean saveAsNewFile(Stage stage, VillageService villageService, RoadService roadService) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存村庄数据文件");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON文件", "*.json")
            );
            
            // 选择村庄保存位置
            File villageFile = fileChooser.showSaveDialog(stage);
            if (villageFile != null) {
                // 保存村庄数据到新文件
                List<Village> villages = villageService.getAllVillages();
                villageService.getVillageDao().saveVillage(villages, villageFile.getAbsolutePath());
                
                // 选择道路保存位置
                fileChooser.setTitle("保存道路数据文件");
                File roadFile = fileChooser.showSaveDialog(stage);
                if (roadFile != null) {
                    List<Road> roads = roadService.getAllRoads();
                    roadService.getRoadDao().saveRoad(roads, roadFile.getAbsolutePath());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("另存为新文件失败", e);
            AlertUtils.showException("保存失败", "无法保存为新文件", e);
            return false;
        }
    }
}
