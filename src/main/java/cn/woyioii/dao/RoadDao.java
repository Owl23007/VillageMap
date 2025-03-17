package cn.woyioii.dao;

import cn.woyioii.model.Road;

import java.util.List;

/**
 * <h1>道路数据访问接口</h1>
 * 定义了道路数据的持久化操作接口，负责村庄间道路数据的存储和检索。
 *
 * <h2>核心功能：</h2>
 * <ul>
 *     <li>保存新道路数据</li>
 *     <li>删除现有道路信息</li>
 *     <li>获取所有道路列表</li>
 *     <li>查询两个村庄间的道路</li>
 * </ul>
 *
 * <h2>实现说明：</h2>
 * <ul>
 *     <li>使用JSON格式存储数据</li>
 *     <li>通过文件系统进行持久化</li>
 *     <li>支持道路数据的CRUD操作</li>
 * </ul>
 *
 * @author woyioii
 * @see Road
 * @see cn.woyioii.dao.impl.RoadDaoImpl
 * @since 1.0
 */
public interface RoadDao {
    //设置文件路径
    void setFilePath(String filePath);
    // 保存道路到文件
    void saveRoad(List<Road> road,String filePath);
    //更新道路到文件
    void updateRoad(List<Road> updatedRoad);
    // 获取所有道路
    List<Road> getAllRoads();
}
