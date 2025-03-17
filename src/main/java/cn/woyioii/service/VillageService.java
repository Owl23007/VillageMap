package cn.woyioii.service;

import cn.woyioii.dao.VillageDao;
import cn.woyioii.model.Village;

import java.util.List;

/**
 * <h1>村庄服务接口</h1>
 * 提供村庄管理的业务逻辑接口，负责协调村庄数据的访问和操作。
 *
 * <h2>核心功能：</h2>
 * <ul>
 *     <li>村庄的增删改查操作</li>
 *     <li>村庄数据的验证和处理</li>
 *     <li>村庄位置的管理</li>
 * </ul>
 *
 * <h2>依赖说明：</h2>
 * <ul>
 *     <li>依赖{@code VillageDao}进行数据访问</li>
 *     <li>与{@code MapRenderer}配合进行地图显示</li>
 *     <li>与{@code RoadService}协同管理道路关系</li>
 * </ul>
 *
 * <h2>使用须知：</h2>
 * <ul>
 *     <li>实现类需要处理数据验证逻辑</li>
 *     <li>确保村庄数据的一致性</li>
 *     <li>需要处理可能的业务异常</li>
 * </ul>
 *
 * @author woyioii
 * @see cn.woyioii.model.Village
 * @see cn.woyioii.dao.VillageDao
 * @since 1.0
 */
public interface VillageService {
    /**
     * 添加新的村庄
     * @param village 村庄实体
     * @return 添加成功返回true，否则返回false
     */
    boolean addVillage(Village village);

    /**
     * 删除指定村庄
     * @param villageId 村庄ID
     * @return 删除成功返回true，否则返回false
     */
    boolean deleteVillage(int villageId);

    /**
     * 更新村庄信息
     * @param village 村庄实体
     * @return 更新成功返回true，否则返回false
     */
    boolean updateVillage(Village village);

    /**
     * 获取所有村庄列表
     * @return 村庄列表
     */
    List<Village> getAllVillages();

    /**
     * 根据ID获取村庄信息
     * @param villageId 村庄ID
     * @return 村庄实体，如果不存在返回null
     */
    Village getVillageById(int villageId);

    /**
     * 验证村庄数据是否有效
     * @param village 村庄实体
     * @return 验证结果，true为有效
     */
    boolean validateVillage(Village village);

    /**
     * 将当前内存中的村庄数据保存到文件
     * 只有在调用此方法时，数据才会写入文件
     */
    void saveVillages();
    
    /**
     * 从文件重新加载村庄数据
     * 会丢弃当前内存中未保存的更改
     */
    void reloadVillages();
    
    /**
     * 获取村庄DAO实例
     * @return 村庄DAO实例
     */
    VillageDao getVillageDao();
}