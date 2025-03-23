package cn.woyioii.util;

import cn.woyioii.model.Road;
import cn.woyioii.model.Village;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 地图计算工具类
 * 提供村庄间距离计算、路径规划、连通性分析等功能
 */
@Slf4j
public class MapCalculator {

    private MapCalculator() {
        // 工具类私有构造
    }

    /**
     * 计算两个村庄之间的直线距离
     */
    public static double calculateDistance(Village village1, Village village2) {
        int x1 = village1.getLocateX();
        int y1 = village1.getLocateY();
        int x2 = village2.getLocateX();
        int y2 = village2.getLocateY();
        
        return calculateDistance( x1,  y1,  x2,  y2);
    }

    /**
     * 计算两点间的直线距离
     */
    public static double calculateDistance(int x1, int y1, int x2, int y2) {
        // 除以十并保留一位小数
        double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y1 - y2, 2));
        return Math.round(distance / 10.0 * 10.0) / 10.0;
    }

    /**
     * 检查图的连通性并返回连通分量
     * @param adjacencyMatrix 邻接矩阵
     * @return 连通分量列表,每个分量包含其中的顶点索引
     */
    public static List<Set<Integer>> checkConnectivity(double[][] adjacencyMatrix) {
        try {
            int n = adjacencyMatrix.length;
            DisjointSet ds = new DisjointSet(n);// 未连通的点集合
            // 检查连通性
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (adjacencyMatrix[i][j] > 0) {
                        ds.union(i, j);// 连通,合并两个集合
                    }
                }
            }
            
            // 获取所有连通分量
            Map<Integer, Set<Integer>> components = new HashMap<>();
            for (int i = 0; i < n; i++) {
                int root = ds.find(i); // 查找根节点
                components.computeIfAbsent(root, k -> new HashSet<>()).add(i);// 添加到对应的连通分量
            }
            
            return new ArrayList<>(components.values());
        } catch (Exception e) {
            log.error("检查连通性时发生错误: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 生成最小生成树连通所有顶点所需最少新边
     * @param adjacencyMatrix 邻接矩阵
     * @param villages 村庄列表
     * @return 需要新增的边列表,每条边用一个长度为2的数组表示[起点id, 终点id]
     */
    public static List<int[]> addNewRoadToConnect(double[][] adjacencyMatrix, List<Village> villages) {
        try {
            int n = adjacencyMatrix.length;
            List<Edge> edges = new ArrayList<>();

            // 生成所有原图中不存在的边
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (adjacencyMatrix[i][j] == 0) { // 只处理原图不存在的边
                        Village v1 = villages.get(i);
                        Village v2 = villages.get(j);
                        double distance = calculateDistance(v1, v2);
                        edges.add(new Edge(i, j, distance));
                    }
                }
            }

            // 按权重排序
            edges.sort(Comparator.comparingDouble(e -> e.weight));

            // 初始化DisjointSet并合并原图已存在的边
            DisjointSet ds = new DisjointSet(n);
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (adjacencyMatrix[i][j] != 0) {
                        ds.union(i, j);
                    }
                }
            }

            List<int[]> newRoads = new ArrayList<>();

            // Kruskal算法选择需要添加的边
            for (Edge edge : edges) {
                int rootStart = ds.find(edge.start);
                int rootEnd = ds.find(edge.end);
                if (rootStart != rootEnd) {
                    newRoads.add(new int[]{edge.start+1, edge.end+1});
                    ds.union(edge.start, edge.end);
                }
            }

            return newRoads;
        } catch (Exception e) {
            log.error("生成连通方案时发生错误: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Floyd-Warshall算法计算所有点对最短路径
     * @param adjacencyMatrix 邻接矩阵
     * @return 距离矩阵
     */
    public static double[][] findAllPairsShortestPaths(double[][] adjacencyMatrix) {
        try {
            int n = adjacencyMatrix.length;
            double[][] dist = new double[n][n];
            
            // 初始化距离矩阵
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) {
                        dist[i][j] = 0;
                    } else if (adjacencyMatrix[i][j] > 0) {
                        dist[i][j] = adjacencyMatrix[i][j];
                    } else {
                        dist[i][j] = Double.POSITIVE_INFINITY;
                    }
                }
            }
            
            // Floyd-Warshall算法
            for (int k = 0; k < n; k++) {
                for (int i = 0; i < n; i++) {
                    if (i != k && !Double.isInfinite(dist[i][k])) {
                        for (int j = 0; j < n; j++) {
                            if (j != k && j != i && !Double.isInfinite(dist[k][j])) {
                                double newDist = dist[i][k] + dist[k][j];
                                if (newDist < dist[i][j]) {
                                    dist[i][j] = newDist;
                                }
                            }
                        }
                    }
                }
            }
            
            return dist;
        } catch (Exception e) {
            log.error("计算所有点对最短路径时发生错误: {}", e.getMessage());
            return new double[0][0];
        }
    }

    /**
     * 计算从指定起点到其他所有顶点的最短路径
     * @param adjacencyMatrix 邻接矩阵
     * @param startVertex 起点索引
     * @return Map<终点索引, 路径顶点列表>
     */
    public static Map<Integer, List<Integer>> findAllPairsShortestPathsWithRoute(double[][] adjacencyMatrix, int startVertex) {
        try {
            int n = adjacencyMatrix.length;
            double[] dist = new double[n];  // 到各点的距离
            int[] prev = new int[n];        // 前驱节点
            boolean[] visited = new boolean[n]; // 访问标记
            
            // 初始化距离和前驱数组
            Arrays.fill(dist, Double.POSITIVE_INFINITY);
            Arrays.fill(prev, -1);
            dist[startVertex] = 0;
            
            // Dijkstra算法
            for (int i = 0; i < n; i++) {
                // 找到未访问的最近顶点
                int u = -1;
                double minDist = Double.POSITIVE_INFINITY;
                for (int j = 0; j < n; j++) {
                    if (!visited[j] && dist[j] < minDist) {
                        u = j;
                        minDist = dist[j];
                    }
                }
                
                if (u == -1 || minDist == Double.POSITIVE_INFINITY) {
                    break;
                }
                
                visited[u] = true;
                
                // 更新通过u能到达的顶点的距离
                for (int v = 0; v < n; v++) {
                    if (!visited[v] && adjacencyMatrix[u][v] > 0) {
                        double newDist = dist[u] + adjacencyMatrix[u][v];
                        if (newDist < dist[v]) {
                            dist[v] = newDist;
                            prev[v] = u;
                        }
                    }
                }
            }
            
            // 重建所有路径
            Map<Integer, List<Integer>> paths = new HashMap<>();
            for (int i = 0; i < n; i++) {
                if (i != startVertex && !Double.isInfinite(dist[i])) {
                    List<Integer> path = new ArrayList<>();
                    for (int curr = i; curr != -1; curr = prev[curr]) {
                        path.addFirst(curr);
                    }
                    paths.put(i, path);
                }
            }
            
            return paths;
        } catch (Exception e) {
            log.error("计算最短路径时发生错误: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 状态压缩DP求解TSP问题,找出经过所有顶点的最短路径
     * @param adjacencyMatrix 邻接矩阵
     * @param startVertex 起点索引
     * @return 路径顶点序列(1-based顶点编号)
     */
    public static List<Integer> findOptimalRoute(double[][] adjacencyMatrix, int startVertex) {
        try {
            if(isStronglyConnected(adjacencyMatrix)) {
                return new ArrayList<>();
            }

            int n = adjacencyMatrix.length;
            double[][] dist = new double[n][n];
            int[][] next = new int[n][n];

            // 初始化距离矩阵和下一跳矩阵
            for(int i = 0; i < n; i++) {
                Arrays.fill(dist[i], Double.POSITIVE_INFINITY);
                Arrays.fill(next[i], -1);
                dist[i][i] = 0;
                for(int j = 0; j < n; j++) {
                    if(adjacencyMatrix[i][j] > 0) {
                        dist[i][j] = adjacencyMatrix[i][j];
                        next[i][j] = j;
                    }
                }
            }

            // Floyd-Warshall求所有点对最短路径
            for(int k = 0; k < n; k++) {
                for(int i = 0; i < n; i++) {
                    for(int j = 0; j < n; j++) {
                        if(dist[i][k] + dist[k][j] < dist[i][j]) {
                            dist[i][j] = dist[i][k] + dist[k][j];
                            next[i][j] = next[i][k];
                        }
                    }
                }
            }

            // 使用贪心算法构建最短路径
            List<Integer> path = new ArrayList<>();
            Set<Integer> visited = new HashSet<>();
            int current = startVertex;
            path.add(current + 1);  // 转换为1-based
            visited.add(current);

            // 每次选择最近的未访问顶点
            while(visited.size() < n) {
                double minDist = Double.POSITIVE_INFINITY;
                int nextVertex = -1;

                for(int i = 0; i < n; i++) {
                    if(!visited.contains(i)) {
                        if(dist[current][i] < minDist) {
                            minDist = dist[current][i];
                            nextVertex = i;
                        }
                    }
                }

                if(nextVertex == -1) {
                    break;  // 无法访问更多顶点
                }

                // 添加最短路径上的所有顶点
                int u = current;
                while(u != nextVertex) {
                    int v = next[u][nextVertex];
                    if(v != nextVertex) {
                        path.add(v + 1);  // 转换为1-based
                    }
                    u = v;
                }
                path.add(nextVertex + 1);  // 转换为1-based

                current = nextVertex;
                visited.add(current);
            }

            return path;
        } catch(Exception e) {
            log.error("计算最优路径时发生错误: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 检验图的强连通性
     * @return true 如果图强连通,false否则
     */
    public static boolean isStronglyConnected(double[][] adjacencyMatrix) {
        try {
            List<Set<Integer>> components = checkConnectivity(adjacencyMatrix);
            return components.size() != 1;
        } catch(Exception e) {
            log.error("检查图强连通性时发生错误: {}", e.getMessage());
            return true;
        }
    }
    
    // 并查集辅助类,用于连通性检查和最小生成树
    private static class DisjointSet {
        private final int[] parent;
        private final int[] rank;

        // 初始化并查集
        DisjointSet(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }

        // 查找根节点
        int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // 路径压缩
            }
            return parent[x];
        }

        // 合并两个集合
        void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            if (rootX != rootY) {
                if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
            }
        }
    }

    // 边结构辅助类,用于Kruskal算法
    private static class Edge {
        int start; // 起点
        int end;   // 终点
        double weight; // 权重（距离）

        public Edge(int start, int end, double weight) {
            this.start = start;
            this.end = end;
            this.weight = weight;
        }
    }

    /**
     * 最小生成树计算结果
     */
    @Getter
    public static class MinSpanningTreeResult {
        private final double totalWeight;
        private final List<Edge> edges;

        public MinSpanningTreeResult(double totalWeight, List<Edge> edges) {
            this.totalWeight = totalWeight;
            this.edges = edges;
        }

    }

    /**
     * 验证邻接矩阵的有效性
     * @param adjacencyMatrix 待验证的邻接矩阵 
     * @return true 如果有效，false 如果无效
     */
    public static boolean validateAdjacencyMatrix(double[][] adjacencyMatrix) {
        if (adjacencyMatrix == null || adjacencyMatrix.length == 0) {
            return false;
        }
        int n = adjacencyMatrix.length;
        for (double[] row : adjacencyMatrix) {
            if (row == null || row.length != n) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算路径总长度
     * @param path 路径顶点序列
     * @param adjacencyMatrix 邻接矩阵
     * @return 路径总长度，如果路径无效则返回-1
     */
    public static double calculatePathLength(List<Integer> path, double[][] adjacencyMatrix) {
        if (path == null || path.size() < 2 || !validateAdjacencyMatrix(adjacencyMatrix)) {
            return -1;
        }
        
        double totalLength = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            int current = path.get(i) - 1; // 转换到0-based索引
            int next = path.get(i + 1) - 1;
            
            if (current < 0 || next < 0 || current >= adjacencyMatrix.length || 
                next >= adjacencyMatrix.length || adjacencyMatrix[current][next] == 0) {
                return -1;
            }
            totalLength += adjacencyMatrix[current][next];
        }
        return Math.round(totalLength * 10.0) / 10.0; // 保留一位小数
    }

    /**
     * 计算最优路径，包含完整的逻辑处理
     * @param villages 村庄列表
     * @param roads 道路列表
     * @param startVillage 起点村庄
     * @return 路径计算结果
     */
    public static RouteResult calculateOptimalRoute(List<Village> villages, List<Road> roads, Village startVillage) {
        RouteResult result = new RouteResult();
        result.setStartVillage(startVillage);
        
        // 基本验证
        if (villages.isEmpty() || roads.isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage("没有可用的村庄或道路数据");
            return result;
        }
        
        // 验证起点是否与任何道路相连
        boolean hasConnectedRoad = roads.stream().anyMatch(road -> 
            road.getStartId() == startVillage.getId() || 
            road.getEndId() == startVillage.getId());
        
        if (!hasConnectedRoad) {
            result.setSuccess(false);
            result.setErrorMessage("起点村庄没有与任何其他村庄相连的道路");
            return result;
        }

        // 构建邻接矩阵
        double[][] adjacencyMatrix = new double[villages.size()][villages.size()];
        for (Road road : roads) {
            int startIndex = -1;
            int endIndex = -1;
            for (int i = 0; i < villages.size(); i++) {
                Village v = villages.get(i);
                if (v.getId() == road.getStartId()) startIndex = i;
                if (v.getId() == road.getEndId()) endIndex = i;
                if (startIndex != -1 && endIndex != -1) break;
            }
            if (startIndex != -1 && endIndex != -1) {
                adjacencyMatrix[startIndex][endIndex] = road.getLength();
                adjacencyMatrix[endIndex][startIndex] = road.getLength();
            }
        }
        
        // 检查连通性
        if (isStronglyConnected(adjacencyMatrix)) {
            // 找到未连通的组
            List<Set<Integer>> components = checkConnectivity(adjacencyMatrix);
            StringBuilder errorMsg = new StringBuilder("村庄网络不完全连通。\n当前存在 ")
                .append(components.size())
                .append(" 个独立的村庄组：\n");
                
            for (int i = 0; i < components.size(); i++) {
                errorMsg.append("组 ").append(i + 1).append(": ");
                components.get(i).forEach(idx -> 
                    errorMsg.append(villages.get(idx).getName()).append("、"));
                errorMsg.setLength(errorMsg.length() - 1);  // 移除最后的顿号
                errorMsg.append("\n");
            }
            
            result.setSuccess(false);
            result.setErrorMessage(errorMsg.toString());
            return result;
        }
        
        int startIndex = villages.indexOf(startVillage);
        List<Integer> optimalPath = findOptimalRoute(adjacencyMatrix, startIndex);
        
        if (optimalPath.isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage("无法找到使用现有道路的有效路径");
            return result;
        }
        
        // 验证路径有效性
        for (int i = 0; i < optimalPath.size() - 1; i++) {
            int currentId = villages.get(optimalPath.get(i) - 1).getId();
            int nextId = villages.get(optimalPath.get(i + 1) - 1).getId();
            boolean hasRoad = roads.stream().anyMatch(road -> 
                (road.getStartId() == currentId && road.getEndId() == nextId) ||
                (road.getStartId() == nextId && road.getEndId() == currentId)
            );
            if (!hasRoad) {
                result.setSuccess(false);
                result.setErrorMessage("计算出的路径包含不存在的道路");
                return result;
            }
        }
        
        // 计算总距离
        double totalDistance = calculatePathLength(optimalPath, adjacencyMatrix);
        if (totalDistance < 0) {
            result.setSuccess(false);
            result.setErrorMessage("无法计算路径总长度");
            return result;
        }
        
        // 转换为村庄对象列表
        List<Village> pathVillages = optimalPath.stream()
                .map(id -> villages.get(id - 1))
                .collect(Collectors.toList());
        
        // 获取路径上的道路
        List<Road> pathRoads = new ArrayList<>();
        for (int i = 0; i < optimalPath.size() - 1; i++) {
            int currentId = villages.get(optimalPath.get(i) - 1).getId();
            int nextId = villages.get(optimalPath.get(i + 1) - 1).getId();
            roads.stream()
                .filter(road -> 
                    (road.getStartId() == currentId && road.getEndId() == nextId) ||
                    (road.getStartId() == nextId && road.getEndId() == currentId))
                .findFirst()
                .ifPresent(pathRoads::add);
        }
        
        // 设置结果
        result.setSuccess(true);
        result.setPath(pathVillages);
        result.setPathRoads(pathRoads);
        result.setTotalDistance(totalDistance);
        result.setIndex(optimalPath);
        
        return result;
    }

    /**
     * 计算最优回路，包含完整的逻辑处理
     * @param villages 村庄列表
     * @param roads 道路列表
     * @param startVillage 起点村庄
     * @return 回路计算结果
     */
    public static RouteResult calculateOptimalRoundTrip(double[][] adjacencyMatrix, List<Village> villages, List<Road> roads, Village startVillage) {
        RouteResult result = new RouteResult();
        result.setStartVillage(startVillage);
        result.setRoundTrip(true);

        log.info("计算最优回路，包含完整的逻辑处理");

        // 基础验证
        if (villages.isEmpty() || roads.isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage("没有可用的村庄或道路数据");
            log.warn("没有可用的村庄或道路数据");
            return result;
        }
        
        // 检查连通性
        if (isStronglyConnected(adjacencyMatrix)) {
            result.setSuccess(false);
            log.warn("村庄网络不完全连通，无法形成环游路线");
            result.setErrorMessage("村庄网络不完全连通，无法形成环游路线");
            return result;
        }
        
        int startIndex = villages.indexOf(startVillage);
        List<Integer> roundTripPath = findOptimalRoundTrip(adjacencyMatrix, startIndex);
        log.info("计算最优回路路径：{}", roundTripPath);
        if (roundTripPath.isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage("无法找到有效的环游路线");
            return result;
        }
        
        // 转换为村庄对象列表
        List<Village> pathVillages = roundTripPath.stream()
                .map(id -> villages.get(id - 1))
                .collect(Collectors.toList());
        
        // 获取路径上的道路
        List<Road> pathRoads = new ArrayList<>();
        for (int i = 0; i < roundTripPath.size() - 1; i++) {
            int currentId = villages.get(roundTripPath.get(i) - 1).getId();
            int nextId = villages.get(roundTripPath.get(i + 1) - 1).getId();
            roads.stream()
                .filter(road ->
                    (road.getStartId() == currentId && road.getEndId() == nextId) ||
                    (road.getStartId() == nextId && road.getEndId() == currentId))
                .findFirst()
                .ifPresent(pathRoads::add);
        }
        
        // 设置结果
        result.setSuccess(true);
        result.setPath(pathVillages);
        result.setPathRoads(pathRoads);
        result.setIndex(roundTripPath);
        
        return result;
    }
    /**
     * 状态压缩DP求解TSP回路问题，找出经过所有顶点的最短环路
     * @param adjacencyMatrix 邻接矩阵
     * @param startIndex 起始顶点索引
     * @return 路径顶点序列(1-based顶点编号)
     */
    public static List<Integer> findOptimalRoundTrip(double[][] adjacencyMatrix, int startIndex) {
        log.info("状态压缩DP求解TSP回路问题，找出经过所有顶点的最短环路");
        List<Integer> result = findOptimalRoute(adjacencyMatrix, startIndex);
        System.out.println(result);

       Map<Integer, List<Integer>> list = findAllPairsShortestPathsWithRoute(adjacencyMatrix, result.getLast()-1);
        System.out.println(startIndex);
        System.out.println(list);
       List<Integer> reversePath = list.get(startIndex);
        System.out.println(reversePath);
        if (reversePath == null || reversePath.isEmpty()) {
            return result;
        }
        log.info("返回路径：{}", reversePath);
        for (Integer integer : reversePath) {
            result.add(integer + 1);
        }

        // 使用 ListIterator 合并连续相同的节点
        ListIterator<Integer> iterator = result.listIterator();
        Integer last = null;
        while (iterator.hasNext()) {
            Integer current = iterator.next();
            if (current.equals(last)) {
                iterator.remove();
            } else {
                last = current;
            }
        }
        return result;
    }
    /**
     * 路径计算结果类
     */
    @Getter
    @Setter
    public static class RouteResult {
        private boolean success;                // 计算是否成功
        private String errorMessage;            // 错误信息
        private List<Village> path;             // 路径村庄列表
        private List<Road> pathRoads;           // 路径上的道路
        private double totalDistance;           // 总距离
        private List<Integer> index;            // 路径索引
        private Village startVillage;           // 起点村庄
        private boolean isRoundTrip;            // 是否为回路
        
        public RouteResult() {
            this.success = false;
            this.isRoundTrip = false;
        }
        
        /**
         * 获取格式化的路径文本
         */
        public String getPathString() {
            if (!success || path == null || path.isEmpty()) {
                return "无有效路径";
            }
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < path.size(); i++) {
                sb.append(path.get(i).getName());
                if (i < path.size() - 1) {
                    sb.append(" → ");
                }
            }
            
            if (isRoundTrip) {
                sb.append(" → ").append(startVillage.getName());
            }
            
            return sb.toString();
        }
        
        /**
         * 获取完整的结果描述文本
         */
        public String getResultText() {
            StringBuilder result = new StringBuilder();
            
            if (!success) {
                result.append("计算失败: ").append(errorMessage);
                return result.toString();
            }
            
            result.append(isRoundTrip ? "最优回路计算结果：\n" : "最优路径计算结果：\n");
            result.append("起点: ").append(startVillage.getName()).append("\n");
            result.append(isRoundTrip ? "回路: " : "路径: ").append(getPathString());
            result.append("\n总距离: ").append(String.format("%.1f", totalDistance)).append("km");
            
            return result.toString();
        }
    }
}
