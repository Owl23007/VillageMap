module cn.woyioii.villageMap {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires com.google.gson;
    requires org.slf4j;
    requires transitive javafx.graphics;

    opens cn.woyioii.model to com.google.gson, javafx.base;
    opens cn.woyioii.controller to javafx.fxml;
    opens cn.woyioii.dao to com.google.gson;

    exports cn.woyioii;
    exports cn.woyioii.controller;
    exports cn.woyioii.service;
    exports cn.woyioii.util;
    exports cn.woyioii.model;
    exports cn.woyioii.handler;
    exports cn.woyioii.dao;
}