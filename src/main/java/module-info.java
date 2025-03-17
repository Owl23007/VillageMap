module cn.woyioii.villagemap {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires com.google.gson;
    requires org.slf4j;

    opens cn.woyioii.model to com.google.gson, javafx.base;
    opens cn.woyioii.controller to javafx.fxml;

    opens images;

    exports cn.woyioii;
    exports cn.woyioii.controller;
    exports cn.woyioii.service;
    exports cn.woyioii.util;
    exports cn.woyioii.model;
    exports cn.woyioii.handler;
}