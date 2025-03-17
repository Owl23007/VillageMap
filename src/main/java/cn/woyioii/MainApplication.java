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
            e.printStackTrace();
        }
    }

    @Override
    public void init() throws Exception {
        try {
            super.init();
            // 初始化数据访问层（DAO）
            VillageDao villageDao = new VillageDaoImpl();
            RoadDao roadDao = new RoadDaoImpl("data/roads.json");

            // 初始化服务层（Service）
            villageService = new VillageServiceImpl(villageDao);
            roadService = new RoadServiceImpl(roadDao, villageService);
            log.info("服务层初始化成功");
        } catch (Exception e) {
            log.error("初始化失败", e);
            throw e;
        }
    }

    // 启动 JavaFX 界面
    @Override
    public void start(Stage primaryStage) {
        // 设置全局异常处理
        ErrorHandler.setupGlobalExceptionHandler();
        
        log.info("启动主界面");
        try {
            // 加载主界面 FXML 文件
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
            log.info("加载主界面");
            
            Parent root = loader.load();
            log.info("FXML加载成功");

            // 获取控制器并注入依赖
            MainController controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("无法获取MainController实例");
            }
            
            controller.setVillageService(villageService);
            controller.setRoadService(roadService);
            log.info("依赖注入完成");
            
            // 初始化数据
            controller.initializeData();
            log.info("数据初始化完成");

            // 配置主窗口
            primaryStage.setTitle("公路村村通系统");
            
            // 使用新的ImageUtils工具类加载图标
            Image appIcon = ImageUtils.loadImage("/images/app_icon.png");
            if (appIcon != null) {
                primaryStage.getIcons().add(appIcon);
            } else {
                log.warn("无法加载应用程序图标");
            }
            
            Scene scene = new Scene(root, 1200, 800);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true); // 默认最大化窗口
            primaryStage.show();
            log.info("主界面加载完成");

        } catch (IOException e) {
            log.error("FXML加载失败", e);
            AlertUtils.showError("FXML加载失败",
                    "错误详情: " + e.getMessage() + "\n" +
                    "原因: " + (e.getCause() != null ? e.getCause().getMessage() : "未知"));
            System.exit(1);
        } catch (Exception e) {
            log.error("启动失败", e);
            ErrorHandler.handleException("界面初始化失败", e);
            System.exit(1);
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
