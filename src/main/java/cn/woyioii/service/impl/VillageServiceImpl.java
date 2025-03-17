package cn.woyioii.service.impl;

import cn.woyioii.dao.VillageDao;
import cn.woyioii.model.Village;
import cn.woyioii.service.VillageService;
import cn.woyioii.util.AlertUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

@Slf4j
public class VillageServiceImpl implements VillageService {
    private final VillageDao villageDao;
    // 内存中存储当前村庄数据
    private List<Village> villages;
    private Consumer<Void> onDataChanged;

    // 依赖注入
    public VillageServiceImpl(VillageDao villageDao) {
        this.villageDao = villageDao;
        try {
            // 初始化时加载数据
            this.villages = new ArrayList<>(villageDao.getAllVillages());
        } catch (Exception e) {
            log.error("初始化村庄数据失败", e);
            this.villages = new ArrayList<>();
        }
    }

    private void notifyDataChanged() {
        if (onDataChanged != null) {
            onDataChanged.accept(null);
        }
    }

    @Override
    public boolean addVillage(Village village) {
        try {
            // 更详细的日志记录
            log.debug("尝试添加村庄: {}", village);
            
            if (!validateVillage(village)) {
                log.warn("村庄验证失败: {}", village);
                AlertUtils.showWarning("添加失败", "村庄信息验证失败，请检查输入");
                return false;
            }
            
            if (villages.stream().anyMatch(v -> v.getId() == village.getId())) {
                log.warn("村庄ID已存在: {}", village.getId());
                AlertUtils.showWarning("添加失败", "村庄ID已存在");
                return false;
            }
            
            villages.add(village);
            // 不再立即写入文件
            log.info("村庄添加成功: {}", village);
            AlertUtils.showInfo("添加成功", "村庄信息已添加");
            notifyDataChanged(); // 通知数据已更改
            return true;
        } catch (Exception e) {
            log.error("添加村庄失败: {}，错误: {}", village, e.getMessage(), e);
            AlertUtils.showException("系统错误", "添加村庄时发生错误: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteVillage(int villageId) {
        try {
            boolean removed = villages.removeIf(v -> v.getId() == villageId);
            if (removed) {
                // 不再立即写入文件
                AlertUtils.showInfo("删除成功", "村庄已删除");
                notifyDataChanged(); // 通知数据已更改
            }
            return removed;
        } catch (Exception e) {
            log.error("删除村庄失败: {}", villageId, e);
            AlertUtils.showException("系统错误", "删除村庄时发生错误", e);
            return false;
        }
    }

    @Override
    public boolean updateVillage(Village village) {
        try {
            if (!validateVillage(village)) {
                AlertUtils.showWarning("更新失败", "村庄信息验证失败，请检查输入");
                return false;
            }
            boolean updated = false;
            for (int i = 0; i < villages.size(); i++) {
                if (villages.get(i).getId() == village.getId()) {
                    villages.set(i, village);
                    updated = true;
                    break;
                }
            }
            if (updated) {
                // 不再立即写入文件
                AlertUtils.showInfo("更新成功", "村庄信息已更新");
                notifyDataChanged(); // 通知数据已更改
            }
            return updated;
        } catch (Exception e) {
            log.error("更新村庄失败: {}", village, e);
            AlertUtils.showException("系统错误", "更新村庄时发生错误", e);
            return false;
        }
    }

    @Override
    public List<Village> getAllVillages() {
        try {
            // 返回内存中的数据而不是重新从文件加载
            return Collections.unmodifiableList(villages);
        } catch (Exception e) {
            log.error("获取村庄列表失败", e);
            AlertUtils.showException("系统错误", "获取村庄列表时发生错误", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Village getVillageById(int villageId) {
        try {
            return villages.stream()
                    .filter(v -> v.getId() == villageId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("获取村庄失败: {}", villageId, e);
            AlertUtils.showException("系统错误", "获取村庄信息时发生错误", e);
            return null;
        }
    }

    @Override
    public boolean validateVillage(Village village) {
        return village != null &&
                village.getId() > 0 &&
                village.getName() != null &&
                !village.getName().trim().isEmpty() &&
                village.getLocateX() >= 0 &&
                village.getLocateY() >= 0;
    }
    
    @Override
    public void saveVillages() {
        try {
            // 显式方法用于保存数据
            villageDao.updateVillage(villages);
            log.info("保存村庄数据成功，共{}个村庄", villages.size());
        } catch (Exception e) {
            log.error("保存村庄数据失败", e);
            AlertUtils.showException("保存失败", "无法保存村庄数据", e);
        }
    }

    @Override
    public void reloadVillages() {
        try {
            // 重新加载数据
            villages.clear();
            villages.addAll(villageDao.getAllVillages());
            log.info("重新加载村庄数据成功，共{}个村庄", villages.size());
        } catch (Exception e) {
            log.error("重新加载村庄数据失败", e);
            AlertUtils.showException("加载失败", "无法重新加载村庄数据", e);
        }
    }

    @Override
    public VillageDao getVillageDao() {
        return villageDao;
    }
}