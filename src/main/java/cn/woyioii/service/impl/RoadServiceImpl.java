package cn.woyioii.service.impl;

import cn.woyioii.dao.RoadDao;
import cn.woyioii.model.Road;
import cn.woyioii.model.Village;
import cn.woyioii.service.RoadService;
import cn.woyioii.service.VillageService;
import cn.woyioii.util.AlertUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RoadServiceImpl implements RoadService {
    private final RoadDao roadDao;
    private final VillageService villageService;
    private List<Road> roads;

    public RoadServiceImpl(RoadDao roadDao, VillageService villageService) {
        this.roadDao = roadDao;
        this.villageService = villageService;
        try {
            // 初始化时加载数据
            this.roads = new ArrayList<>(roadDao.getAllRoads());
        } catch (Exception e) {
            log.error("初始化道路数据失败", e);
            this.roads = new ArrayList<>();
        }
    }

    /**
     * 添加道路
     * 包含验证逻辑：
     * 1. 检查起点终点村庄是否存在
     * 2. 检查是否已存在相同道路
     * 3. 自动计算道路长度
     */
    @Override
    public boolean addRoad(Road road) {
        try {
            // 检查道路基本信息是否有效
            if (!validateRoad(road)) {
                AlertUtils.showWarning("添加失败", "道路信息不完整或无效，请检查输入");
                return false;
            }

            // 检查是否已存在连接相同两个村庄的道路（不分方向）
            boolean existingRoad = roads.stream().anyMatch(r -> 
                (r.getStartId().equals(road.getStartId()) && r.getEndId().equals(road.getEndId())) ||
                (r.getStartId().equals(road.getEndId()) && r.getEndId().equals(road.getStartId()))
            );
            
            if (existingRoad) {
                AlertUtils.showWarning("添加失败", "连接这两个村庄的道路已存在");
                return false;
            }

            // 生成新的唯一ID
            int maxId = roads.stream()
                    .mapToInt(r -> r.getId() != null ? r.getId() : 0)
                    .max()
                    .orElse(0);
            road.setId(maxId + 1);
            
            roads.add(road);
            log.info("道路添加成功: {}", road);
            return true;
        } catch (Exception e) {
            log.error("添加道路失败: {}", road, e);
            AlertUtils.showException("系统错误", "添加道路时发生错误", e);
            return false;
        }
    }

    @Override
    public boolean deleteRoad(int roadId) {
        try {
            boolean removed = roads.removeIf(r -> r.getId().equals(roadId));
            if (removed) {
                // 不再立即写入文件
                AlertUtils.showInfo("删除成功", "道路已删除");
            }
            return removed;
        } catch (Exception e) {
            log.error("删除道路失败: {}", roadId, e);
            AlertUtils.showException("系统错误", "删除道路时发生错误", e);
            return false;
        }
    }

    @Override
    public boolean deleteRoad(Road road) {
        if (road == null || road.getId() == null) {
            return false;
        }
        return deleteRoad(road.getId());
    }

    @Override
    public boolean updateRoad(Road road) {
        try {
            if (!validateRoad(road)) {
                AlertUtils.showWarning("更新失败", "道路信息验证失败，请检查输入");
                return false;
            }
            boolean updated = false;
            for (int i = 0; i < roads.size(); i++) {
                if (roads.get(i).getId().equals(road.getId())) {
                    roads.set(i, road);
                    updated = true;
                    break;
                }
            }
            if (updated) {
                // 不再立即写入文件
                AlertUtils.showInfo("更新成功", "道路信息已更新");
            }
            return updated;
        } catch (Exception e) {
            log.error("更新道路失败: {}", road, e);
            AlertUtils.showException("系统错误", "更新道路时发生错误", e);
            return false;
        }
    }

    @Override
    public List<Road> getAllRoads() {
        try {
            // 返回内存中的数据
            return Collections.unmodifiableList(roads);
        } catch (Exception e) {
            log.error("获取道路列表失败", e);
            AlertUtils.showException("系统错误", "获取道路列表时发生错误", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Road getRoadById(Integer roadId) {
        try {
            return roads.stream()
                    .filter(r -> r.getId().equals(roadId))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("获取道路失败: {}", roadId, e);
            AlertUtils.showException("系统错误", "获取道路信息时发生错误", e);
            return null;
        }
    }

    @Override
    public List<Road> getRoadsByVillageId(Integer villageId) {
        try {
            return roads.stream()
                    .filter(r -> Objects.equals(r.getStartId(), villageId) ||
                            Objects.equals(r.getEndId(), villageId))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取村庄相关道路失败: {}", villageId, e);
            AlertUtils.showException("系统错误", "获取村庄相关道路时发生错误", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public boolean validateRoad(Road road) {
        if (road == null || road.getStartId() == null || road.getEndId() == null || 
            road.getLength() <= 0 || road.getName() == null || road.getName().trim().isEmpty()) {
            return false;
        }

        // 验证起点和终点村庄是否存在
        if (villageService.getVillageById(road.getStartId()) == null ||
            villageService.getVillageById(road.getEndId()) == null) {
            AlertUtils.showWarning("验证失败", "道路的起点或终点村庄不存在");
            return false;
        }

        return true;
    }

    /**
     * 计算最短路径
     * 使用Dijkstra算法实现：
     * 1. 构建邻接图
     * 2. 计算最短路径
     * 3. 返回路径上的道路列表
     */
    @Override
    public List<Road> calculateShortestPath(String startVillageId, String endVillageId) {
        try {
            int startId = Integer.parseInt(startVillageId);
            int endId = Integer.parseInt(endVillageId);
            
            log.info("计算从村庄{}到村庄{}的最短路径", startId, endId);
            
            // 验证起点和终点村庄存在
            if (villageService.getVillageById(startId) == null || 
                villageService.getVillageById(endId) == null) {
                log.error("起点或终点村庄不存在");
                AlertUtils.showWarning("参数错误", "起点或终点村庄不存在");
                return Collections.emptyList();
            }
            
            // 如果起点和终点相同，返回空路径
            if (startId == endId) {
                log.info("起点和终点相同，返回空路径");
                return Collections.emptyList();
            }
            
            // 构建图数据结构
            Map<Integer, Map<Integer, Road>> graph = new HashMap<>();
            
            // 填充图
            for (Road road : roads) {
                // 添加正向路径
                graph.computeIfAbsent(road.getStartId(), k -> new HashMap<>())
                    .put(road.getEndId(), road);
                
                // 由于是无向图，同时添加反向路径
                graph.computeIfAbsent(road.getEndId(), k -> new HashMap<>())
                    .put(road.getStartId(), road);
            }
            
            // 无法到达检查
            if (!graph.containsKey(startId) || !graph.containsKey(endId)) {
                log.warn("起点或终点没有连接的道路");
                AlertUtils.showWarning("无法计算", "起点或终点没有连接的道路");
                return Collections.emptyList();
            }
            
            // Dijkstra算法
            Map<Integer, Double> distances = new HashMap<>();
            Map<Integer, Integer> previous = new HashMap<>();
            PriorityQueue<VillageDistance> queue = new PriorityQueue<>(
                Comparator.comparingDouble(VillageDistance::getDistance));
            Set<Integer> visited = new HashSet<>();
            
            // 初始化距离
            for (Village village : villageService.getAllVillages()) {
                if (village.getId() == startId) {
                    distances.put(startId, 0.0);
                    queue.add(new VillageDistance(startId, 0.0));
                } else {
                    distances.put(village.getId(), Double.MAX_VALUE);
                }
                previous.put(village.getId(), null);
            }
            
            // 开始寻路
            while (!queue.isEmpty()) {
                VillageDistance current = queue.poll();
                int currentId = current.getVillageId();
                
                if (visited.contains(currentId)) {
                    continue;
                }
                
                if (currentId == endId) {
                    break; // 到达终点
                }
                
                visited.add(currentId);
                
                // 没有邻接点，跳过
                if (!graph.containsKey(currentId)) {
                    continue;
                }
                
                // 检查所有邻居
                for (Map.Entry<Integer, Road> neighbor : graph.get(currentId).entrySet()) {
                    int neighborId = neighbor.getKey();
                    
                    if (visited.contains(neighborId)) {
                        continue;
                    }
                    
                    Road road = neighbor.getValue();
                    double newDist = distances.get(currentId) + road.getLength();
                    
                    if (newDist < distances.getOrDefault(neighborId, Double.MAX_VALUE)) {
                        distances.put(neighborId, newDist);
                        previous.put(neighborId, currentId);
                        queue.add(new VillageDistance(neighborId, newDist));
                    }
                }
            }
            
            // 重建路径
            List<Road> path = new ArrayList<>();
            Integer current = endId;
            
            // 如果无法到达终点
            if (previous.get(endId) == null && endId != startId) {
                log.warn("无法从村庄{}到达村庄{}", startId, endId);
                AlertUtils.showWarning("无法到达", "没有通往目标村庄的路径");
                return Collections.emptyList();
            }
            
            // 回溯路径
            while (current != null && previous.get(current) != null) {
                Integer prev = previous.get(current);
                Road road = findRoad(prev, current);
                if (road != null) {
                    path.add(0, road); // 添加到路径开头
                }
                current = prev;
            }
            
            log.info("最短路径计算完成，共{}条道路", path.size());
            return path;
        } catch (Exception e) {
            log.error("计算最短路径失败: {} -> {}", startVillageId, endVillageId, e);
            AlertUtils.showException("系统错误", "计算最短路径时发生错误", e);
            return Collections.emptyList();
        }
    }

    // 寻找连接两个村庄的道路
    private Road findRoad(int startId, int endId) {
        for (Road road : roads) {
            // 检查正向路径
            if (road.getStartId() == startId && road.getEndId() == endId) {
                return road;
            }
            // 检查反向路径（无向图）
            if (road.getStartId() == endId && road.getEndId() == startId) {
                return road;
            }
        }
        return null;
    }

    // 用于Dijkstra算法的辅助类
    private static class VillageDistance {
        private final int villageId;
        private final double distance;
        
        public VillageDistance(int villageId, double distance) {
            this.villageId = villageId;
            this.distance = distance;
        }
        
        public int getVillageId() {
            return villageId;
        }
        
        public double getDistance() {
            return distance;
        }
    }

    @Override
    public void saveRoads() {
        try {
            roadDao.updateRoad(roads);
            log.info("保存道路数据成功，共{}条道路", roads.size());
        } catch (Exception e) {
            log.error("保存道路数据失败", e);
            AlertUtils.showException("保存失败", "无法保存道路数据", e);
        }
    }
    
    @Override
    public void reloadRoads() {
        try {
            roads.clear();
            roads.addAll(roadDao.getAllRoads());
            validateRoadReferences(villageService);
            log.info("重新加载道路数据成功，共{}条道路", roads.size());
        } catch (Exception e) {
            log.error("重新加载道路数据失败", e);
            AlertUtils.showException("加载失败", "无法重新加载道路数据", e);
        }
    }

    @Override
    public RoadDao getRoadDao() {
        return this.roadDao;
    }

    @Override
    public boolean hasChanges() {
        if (roadDao == null) {
            return false;
        }
        try {
            List<Road> savedRoads = roadDao.getAllRoads();
            return !roads.equals(savedRoads);
        } catch (Exception e) {
            log.error("检查数据变更失败", e);
            return false;
        }
    }

    /**
     * 验证道路引用的有效性
     * 移除引用不存在村庄的道路
     * @param villageService 村庄服务
     */
    @Override
    public void validateRoadReferences(VillageService villageService) {
        List<Road> roads = getAllRoads();
        List<Road> invalidRoads = new ArrayList<>();
        
        for (Road road : roads) {
            // 检查起点和终点村庄是否存在
            boolean startExists = villageService.getVillageById(road.getStartId()) != null;
            boolean endExists = villageService.getVillageById(road.getEndId()) != null;
            
            if (!startExists || !endExists) {
                invalidRoads.add(road);
                log.warn("发现无效道路引用: {} (起点: {}, 终点: {})", 
                    road.getName(), road.getStartId(), road.getEndId());
            }
        }
        
        // 移除无效道路
        for (Road invalidRoad : invalidRoads) {
            roads.remove(invalidRoad);
            log.info("已移除无效道路: {}", invalidRoad.getName());
        }
        
        // 如果有无效道路被移除，保存更新后的道路数据
        if (!invalidRoads.isEmpty()) {
            saveRoads();
            log.info("已移除 {} 条无效道路引用", invalidRoads.size());
        }
    }

    @Override
    public void createNewRoads() {
        try {
            if (roads == null) {
                roads = new ArrayList<>();
            }
            roads.clear();
            log.info("已创建新的空白道路数据");
        } catch (Exception e) {
            log.error("创建新的道路数据失败", e);
            AlertUtils.showException("创建失败", "无法创建新的道路数据", e);
        }
    }
}