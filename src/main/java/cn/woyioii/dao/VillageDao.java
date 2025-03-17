package cn.woyioii.dao;

import cn.woyioii.model.Village;

import java.util.List;
/**
 * <h1>村庄数据访问接口</h1>
 * 定义了村庄数据的持久化操作接口，负责村庄数据的存储和检索。
 *
 * <h2>核心功能：</h2>
 * <ul>
 *     <li>保存新村庄数据</li>
 *     <li>更新现有村庄信息</li>
 *     <li>获取所有村庄列表</li>
 * </ul>
 *
 * <h2>实现说明：</h2>
 * <ul>
 *     <li>使用JSON格式存储数据</li>
 *     <li>通过文件系统进行持久化</li>
 *     <li>支持村庄数据的CRUD操作</li>
 * </ul>
 *
 * @author woyioii
 * @see Village
 * @see cn.woyioii.dao.impl.VillageDaoImpl
 * @since 1.0
 */
public interface VillageDao {
    //设置当前文件路径
    void setFilePath(String filePath);
    // 保存村庄到文件
    void saveVillage(List<Village> village,String filePath);
    // 更新村庄到文件
    void updateVillage(List<Village> updatedVillage);
    // 从文件中获取所有村庄
    List<Village> getAllVillages();
}
