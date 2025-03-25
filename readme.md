# VillageMap - 乡村道路规划与分析系统

![Java](https://img.shields.io/badge/Java-21-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-21-orange)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

基于JavaFX的图形化乡村道路规划工具，提供村庄与道路管理、路径规划、网络分析等功能。

## 📌 功能特性

### 核心功能
- **数据管理**
  - JSON格式持久化存储
  - 智能文件命名（`[基础名称]-villages.json` / `[基础名称]-roads.json`）
  - 数据有效性验证（道路端点必须存在）

- **路径规划**
  - 🛣️ 最短路径查找（Floyd-Warshall算法）
  - 🔁 最优环线生成（最近邻启发式算法）
  - 🌐 连通性检查（并查集算法）

- **网络分析**
  - 🌳 最小生成树生成（Kruskal算法）
  - 🔧 智能道路建议系统
  - 📊 网络拓扑可视化

### 扩展功能
- 多视图数据展示（表格+图形化）
- 实时状态监控
- 右键快速操作菜单
- 数据修改追踪与提示

## 🚀 快速开始

### 环境要求
- JDK 21+
- Maven 3.6+

### 安装与运行
```bash
git clone https://github.com/Owl23007/VillageMap.git
cd VillageMap
mvn clean 
javafx:run
```

## 🗂️ 项目结构
```
VillageMap/
├── src/
│   ├── main/
│   │   ├── java/cn/woyioii/
│   │   │   ├── controller/ # 控制器
│   │   │   ├── handler/    # 事件处理
│   │   │   ├── model/      # 数据模型
│   │   │   ├── render/     # 地图渲染
│   │   │   ├── service/    # 服务接口
│   │   │   └── util/       # 工具类
│   │   └── resources/      # 资源文件
├── data/                   # 示例数据
└── pom.xml                 # Maven配置
```

## 🔧 技术栈
- **核心框架**: JavaFX 21
- **数据持久化**: Gson
- **工具库**: Lombok, SLF4J
- **算法实现**:
  - Kruskal (最小生成树)
  - Floyd-Warshall (全源最短路径)
  - 最近邻启发式 (TSP近似解)

## 📊 数据格式
```json
// 村庄数据示例
{
  "id": 1,
  "name": "青松镇",
  "locateX": 150,
  "locateY": 120,
  "description": "林业重镇"
}

// 道路数据示例
{
  "id": 1,
  "startId": 1,
  "endId": 13,
  "name": "松林路",
  "length": 8.1
}
```

## 📜 开发文档
### 核心算法流程
```java
// 邻接矩阵构建
public double[][] dataToAdjacencyMatrix(List<Village> villages, List<Road> roads) {
    int n = villages.size();
    double[][] matrix = new double[n][n];
    roads.forEach(road -> {
        int start = getVillageIndex(road.getStartId());
        int end = getVillageIndex(road.getEndId());
        matrix[start][end] = matrix[end][start] = road.getLength();
    });
    return matrix;
}
```

## 📄 许可证
[MIT License](LICENSE)

---
> 课程设计作品 | 开发者：Owl23007 | 技术咨询：mailofowlwork@gmail.com