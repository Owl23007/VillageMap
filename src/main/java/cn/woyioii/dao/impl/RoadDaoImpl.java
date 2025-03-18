package cn.woyioii.dao.impl;

import cn.woyioii.dao.RoadDao;
import cn.woyioii.model.Road;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RoadDaoImpl implements RoadDao {
    private String filePath;
    private final Gson gson = new Gson();

    public RoadDaoImpl() {
        this.filePath = "data/default-roads.json";
        initializeFile(); // 确保文件存在
    }

    public RoadDaoImpl(String filePath) {
        this.filePath = filePath;
        initializeFile();
    }

    @Override
    public void setFilePath(String filePath) {
        // 设置文件路径
        this.filePath = filePath;
        initializeFile();
    }

    @Override
    public void saveRoad(List<Road> road, String filePath) {
        this.filePath = filePath;
        initializeFile();
        // 保存道路到文件
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(road, writer);
        } catch (IOException e) {
            throw new RuntimeException("保存道路数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateRoad(List<Road> updatedRoad) {
        // 更新道路到文件
        saveAllRoads(updatedRoad);
    }

    private void initializeFile() {
        Path path = Paths.get(filePath);
        // 确保文件存在
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
                saveAllRoads(new ArrayList<>());
            } catch (IOException e) {
                throw new RuntimeException("无法初始化道路数据文件: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public List<Road> getAllRoads() {
        try (FileReader reader = new FileReader(filePath)) {
            List<Road> roads = gson.fromJson(reader, new TypeToken<List<Road>>() {}.getType());
            
            // 验证和过滤无效数据
            if (roads == null) {
                return new ArrayList<>();
            }
            
            return roads.stream()
                .filter(road -> road != null && road.getStartId() != null && road.getEndId() != null)
                .collect(Collectors.toList());
                
        } catch (IOException e) {
            log.error("读取道路数据失败", e);
            return new ArrayList<>();
        }
    }

    private void saveAllRoads(List<Road> roads) {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(roads, writer);
        } catch (IOException e) {
            throw new RuntimeException("保存道路数据失败: " + e.getMessage(), e);
        }
    }
}
