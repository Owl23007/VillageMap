package cn.woyioii.util;

import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URL;

/**
 * 图像工具类，用于处理应用程序中的图像资源
 */
@Slf4j
public class ImageUtils {
    
    private ImageUtils() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 加载应用程序资源中的图像
     * 
     * @param resourcePath 图像资源路径
     * @return 加载的图像，如果加载失败则返回null
     */
    public static Image loadImage(String resourcePath) {
        try {
            URL resource = ImageUtils.class.getResource(resourcePath);
            if (resource == null) {
                log.error("找不到图像资源: {}", resourcePath);
                return null;
            }
            
            // 通过输入流加载图像并设置背景加载为false以便立即捕获错误
            try (InputStream is = resource.openStream()) {
                return new Image(is);
            }
        } catch (Exception e) {
            log.error("加载图像失败: {}", resourcePath, e);
            return null;
        }
    }
}
