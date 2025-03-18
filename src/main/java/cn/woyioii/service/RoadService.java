package cn.woyioii.service;

import cn.woyioii.dao.RoadDao;
import cn.woyioii.model.Road;

import java.util.List;


/**
 * <h1>路的服务接口层</h1>
 * 本接口负责系统中道路实体的核心业务管理。
 *
 * <h2>核心功能：</h2>
 * <ul>
 *     <li>道路信息的基础增删改查操作</li>
 *     <li>道路数据的有效性校验</li>
 *     <li>道路与村庄关系的管理</li>
 *     <li>道路数据的批量处理能力</li>
 * </ul>
 *
 * <h2>依赖说明：</h2>
 * <ul>
 *     <li>需要通过 {@code RoadDao} 对象进行初始化</li>
 *     <li>与 {@code VillageService} 协同工作以处理道路-村庄关联</li>
 * </ul>
 *
 * <h2>使用须知：</h2>
 * <ul>
 *     <li>所有操作都具有事务特性</li>
 *     <li>支持道路数据的持久化存储和检索</li>
 *     <li>提供数据完整性保证</li>
 * </ul>
 *
 * @author woyioii
 * @see cn.woyioii.dao.RoadDao
 * @see cn.woyioii.model.Road
 * @since 1.0
 */
public interface RoadService {
    /**
     * 添加新的道路
     * @param road 道路实体
     * @return 添加成功返回true，否则返回false
     */
    boolean addRoad(Road road);

    /**
     * 删除指定道路
     * @param roadId 道路ID
     * @return 删除成功返回true，否则返回false
     */
    boolean deleteRoad(int roadId);
    
    /**
     * 删除指定道路
     * @param road 道路对象
     * @return 删除成功返回true，否则返回false
     */
    boolean deleteRoad(Road road);

    /**
     * 更新道路信息
     * @param road 道路实体
     * @return 更新成功返回true，否则返回false
     */
    boolean updateRoad(Road road);

    /**
     * 获取所有道路列表
     * @return 道路列表
     */
    List<Road> getAllRoads();

    /**
     * 根据ID获取道路信息
     * @param roadId 道路ID
     * @return 道路实体，如果不存在返回null
     */
    Road getRoadById(Integer roadId);

    /**
     * 获取连接指定村庄的所有道路
     * @param villageId 村庄ID
     * @return 道路列表
     */
    List<Road> getRoadsByVillageId(Integer villageId);

    /**
     * 验证道路是否有效
     * @param road 道路实体
     * @return 验证结果，true为有效
     */
    boolean validateRoad(Road road);

    /**
     * 计算两个村庄之间的最短路径
     * @param startVillageId 起始村庄ID
     * @param endVillageId 目标村庄ID
     * @return 路径上的道路列表
     */
    List<Road> calculateShortestPath(String startVillageId, String endVillageId);

    /**
     * 将当前内存中的道路数据保存到文件
     * 只有在调用此方法时，数据才会写入文件
     */
    void saveRoads();
    
    /**
     * 从文件重新加载道路数据
     * 会丢弃当前内存中未保存的更改
     */
    void reloadRoads();
    
    /**
     * 获取道路DAO实例
     * @return 道路DAO实例
     */
    RoadDao getRoadDao();
    
    /**
     * 检查是否有未保存的更改
     * @return 如果有未保存的更改返回true，否则返回false
     */
    boolean hasChanges();
    
    /**
     * 创建新的空白道路数据，清除当前内存中数据
     */
    void createNewRoads();
    
    /**
     * 验证道路数据中的村庄引用是否有效，移除无效引用
     * @param villageService 用于验证村庄ID的服务
     */
    void validateRoadReferences(VillageService villageService);
}

