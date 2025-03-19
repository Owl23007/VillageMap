package cn.woyioii;

import cn.woyioii.controller.FileController;
import cn.woyioii.controller.MainController;
import cn.woyioii.dao.RoadDao;
import cn.woyioii.dao.VillageDao;
import cn.woyioii.dao.impl.RoadDaoImpl;
import cn.woyioii.dao.impl.VillageDaoImpl;
import cn.woyioii.service.RoadService;
import cn.woyioii.service.VillageService;
import cn.woyioii.service.impl.RoadServiceImpl;
import cn.woyioii.service.impl.VillageServiceImpl;
import cn.woyioii.handler.ErrorHandler;
import cn.woyioii.util.ImageUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import cn.woyioii.util.AlertUtils;

import java.io.IOException;

/**
 * 程序入口类，继承 JavaFX 的 Application
 */

@Slf4j
public class MainApplication extends Application {
    private VillageService villageService;
    private RoadService roadService;

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("程序启动失败: " + e.getMessage());
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void init() throws Exception {
        try {
            super.init();
            // 初始化数据访问层（DAO）
            VillageDao villageDao = new VillageDaoImpl();
            RoadDao roadDao = new RoadDaoImpl();

            // 初始化服务层（Service）
            villageService = new VillageServiceImpl(villageDao);
            roadService = new RoadServiceImpl(roadDao, villageService);
        } catch (Exception e) {
            log.error("初始化失败", e);
            throw e;
        }
    }

    // 启动 JavaFX 界面
    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
            Parent root = loader.load();

            // 配置Controller，使用init()中已初始化的服务实例
            MainController controller = loader.getController();
            controller.setServices(villageService, roadService);
            controller.setStage(primaryStage);
            
            // 初始化数据
            controller.initializeData();

            // 设置主场景
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("村庄地图管理系统");
            
            // 添加图标加载错误处理
            Image icon = ImageUtils.loadImage("/images/app_icon.png");
            if (icon != null) {
                primaryStage.getIcons().add(icon);
            } else {
                log.warn("未能加载应用程序图标");
            }
            
            primaryStage.show();
            
        } catch (Exception e) {
            log.error("启动应用程序失败", e);
            AlertUtils.showException("启动失败", "无法启动应用程序", e);
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        // 程序退出前保存数据
        ErrorHandler.safeExecute("保存数据", () -> {
            log.info("应用程序关闭，保存数据");
            FileController.saveAllData(villageService, roadService);
        });
    }
}

