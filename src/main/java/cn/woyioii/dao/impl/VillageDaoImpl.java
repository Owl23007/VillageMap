package cn.woyioii.dao.impl;

import cn.woyioii.model.Village;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import cn.woyioii.dao.VillageDao;

public class VillageDaoImpl implements VillageDao {
    private String filePath;
    private final Gson gson =new Gson();

    public VillageDaoImpl() {
        // 初始化,默认文件路径
        this.filePath = "data/default-villages.json";
        initializeFile(); // 确保文件存在
    }

    private void initializeFile() {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
                saveAllVillages(new ArrayList<>());
            } catch (IOException e) {
                throw new RuntimeException("无法初始化村庄数据文件: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void setFilePath(String filePath) {
        // 设置文件路径
        this.filePath = filePath;
        initializeFile();
    }

    @Override
    public void saveVillage(List<Village> village, String filePath) {
        // 保存村庄到文件
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(village, writer);
        } catch (IOException e) {
            throw new RuntimeException("保存村庄数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateVillage(List<Village> updatedVillage) {
        // 更新村庄到文件
        saveAllVillages(updatedVillage);
    }

    @Override
    public List<Village> getAllVillages(){
        // 获取所有村庄
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, new TypeToken<List<Village>>() {
            }.getType());
        } catch (IOException e) {
            throw new RuntimeException("读取村庄数据失败: " + e.getMessage(), e);
        }
    }

    private void saveAllVillages(List<Village> villages) {
        // 保存所有村庄
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(villages, writer);
        } catch (IOException e) {
            throw new RuntimeException("保存村庄数据失败: " + e.getMessage(), e);
        }
    }
}
