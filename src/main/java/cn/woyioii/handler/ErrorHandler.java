package cn.woyioii.handler;

import cn.woyioii.util.AlertUtils;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理工具类
 * 提供更好的异常捕获、记录和展示功能
 */
@Slf4j
public class ErrorHandler {
    
    private ErrorHandler() {
    }

    /**
     * 处理异常，记录日志并显示给用户
     */
    public static void handleException(String context, Throwable throwable) {
        // 获取根本原因
        Throwable rootCause = getRootCause(throwable);
        
        // 日志记录
        log.error("{}: ", context, throwable);
        log.error("根本原因: {}: {}", rootCause.getClass().getName(), rootCause.getMessage());
        
        // UI线程中显示错误
        if (Platform.isFxApplicationThread()) {
            showErrorDialog(context, rootCause, throwable);
        } else {
            Platform.runLater(() -> showErrorDialog(context, rootCause, throwable));
        }
    }
    
    /**
     * 显示错误对话框
     */
    private static void showErrorDialog(String context, Throwable rootCause, Throwable fullException) {
        AlertUtils.showException("系统错误",
            context + "\n" + 
            "类型: " + rootCause.getClass().getName() + "\n" +
            "消息: " + rootCause.getMessage(), 
            fullException);
    }
    
    /**
     * 获取异常链中的根本原因
     */
    private static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        return cause;
    }
    
    /**
     * 安全执行可能抛出异常的Runnable
     */
    public static void safeExecute(String context, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            handleException(context, e);
        }
    }
}
