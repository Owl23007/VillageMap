package cn.woyioii.util;

import cn.woyioii.model.Village;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

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
            if(!isStronglyConnected(adjacencyMatrix)) {
                return new ArrayList<>(); // 图不连通则返回空路径
            }
            
            int n = adjacencyMatrix.length;
            // 验证路径的可行性（检查是否有边连接）
            boolean[][] hasEdge = new boolean[n][n];
            for(int i = 0; i < n; i++) {
                for(int j = 0; j < n; j++) {
                    hasEdge[i][j] = adjacencyMatrix[i][j] > 0;
                }
            }

            // 动态规划 - 状态压缩
            double[][] dp = new double[1 << n][n];
            int[][] parent = new int[1 << n][n];
            
            for(double[] row : dp) {
                Arrays.fill(row, Double.POSITIVE_INFINITY);
            }
            
            // 初始状态
            dp[1 << startVertex][startVertex] = 0;
            
            // 遍历所有状态
            for(int mask = 0; mask < (1 << n); mask++) {
                for(int last = 0; last < n; last++) {
                    if((mask & (1 << last)) == 0) continue;
                    
                    // 尝试扩展到下一个节点
                    for(int next = 0; next < n; next++) {
                        if((mask & (1 << next)) != 0) continue;
                        if(!hasEdge[last][next]) continue; // 检查边是否存在
                        
                        int nextMask = mask | (1 << next);
                        double newDist = dp[mask][last] + adjacencyMatrix[last][next];
                        
                        if(newDist < dp[nextMask][next]) {
                            dp[nextMask][next] = newDist;
                            parent[nextMask][next] = last;
                        }
                    }
                }
            }
            
            // 重建路径
            List<Integer> path = new ArrayList<>();
            int mask = (1 << n) - 1;
            int last = -1;
            double minDist = Double.POSITIVE_INFINITY;
            
            // 找到终点
            for(int i = 0; i < n; i++) {
                if(dp[mask][i] < minDist) {
                    minDist = dp[mask][i];
                    last = i;
                }
            }
            
            if(last == -1 || Double.isInfinite(minDist)) {
                return new ArrayList<>(); // 无有效路径
            }
            
            // 重建路径
            while(mask != 0) {
                path.addFirst(last + 1); // 转换为1-based索引
                int prev = parent[mask][last];
                mask &= ~(1 << last);
                last = prev;
            }
            
            return path;
        } catch(Exception e) {
            log.error("计算最优路径时发生错误: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 状态压缩DP求解TSP回路问题,找出经过所有顶点并返回起点的最短回路
     * @param adjacencyMatrix 邻接矩阵  
     * @param startVertex 起点索引
     * @return 回路顶点序列(1-based顶点编号,首尾都是起点)
     */
    public static List<Integer> findOptimalRoundTrip(double[][] adjacencyMatrix, int startVertex) {
        try {
            if(!isStronglyConnected(adjacencyMatrix)) {
                return new ArrayList<>();
            }
            
            int n = adjacencyMatrix.length;
            boolean[][] hasEdge = new boolean[n][n];
            for(int i = 0; i < n; i++) {
                for(int j = 0; j < n; j++) {
                    hasEdge[i][j] = adjacencyMatrix[i][j] > 0;
                }
            }
            
            // 动态规划数组
            double[][] dp = new double[1 << n][n];
            int[][] parent = new int[1 << n][n];
            
            for(double[] row : dp) {
                Arrays.fill(row, Double.POSITIVE_INFINITY);
            }
            
            // 初始状态
            dp[1 << startVertex][startVertex] = 0;
            
            // 遍历所有状态
            for(int mask = 0; mask < (1 << n); mask++) {
                for(int last = 0; last < n; last++) {
                    if((mask & (1 << last)) == 0) continue;
                    
                    for(int next = 0; next < n; next++) {
                        if((mask & (1 << next)) != 0) continue;
                        if(!hasEdge[last][next]) continue; // 检查边是否存在
                        
                        int nextMask = mask | (1 << next);
                        double newDist = dp[mask][last] + adjacencyMatrix[last][next];
                        
                        if(newDist < dp[nextMask][next]) {
                            dp[nextMask][next] = newDist;
                            parent[nextMask][next] = last;
                        }
                    }
                }
            }
            
            // 寻找最优回路
            int fullMask = (1 << n) - 1;
            double minTotalDist = Double.POSITIVE_INFINITY;
            int lastVertex = -1;
            
            // 检查从最后一个顶点返回起点是否可行
            for(int last = 0; last < n; last++) {
                if(last != startVertex && hasEdge[last][startVertex]) {
                    double totalDist = dp[fullMask][last] + adjacencyMatrix[last][startVertex];
                    if(totalDist < minTotalDist) {
                        minTotalDist = totalDist;
                        lastVertex = last;
                    }
                }
            }
            
            if(lastVertex == -1 || Double.isInfinite(minTotalDist)) {
                return new ArrayList<>();
            }
            
            // 重建路径
            List<Integer> path = new ArrayList<>();
            int mask = fullMask;
            int curr = lastVertex;
            
            while(mask != 0) {
                path.addFirst(curr + 1);
                int prev = parent[mask][curr];
                mask &= ~(1 << curr);
                curr = prev;
            }
            
            // 添加起点作为路径的第一个点和最后一个点
            path.addFirst(startVertex + 1);
            if(!path.isEmpty()) {
                path.add(startVertex + 1);
            }
            
            return path;
        } catch(Exception e) {
            log.error("计算最优回路时发生错误: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 多源最短路径查询,优化版Floyd-Warshall算法
     * @param adjacencyMatrix 邻接矩阵
     * @return Map<起点ID-终点ID, 最短距离>
     */
    public static Map<String, Double> findAllPairsShortestPathsOptimized(double[][] adjacencyMatrix) {
        try {
            int n = adjacencyMatrix.length;
            Map<String, Double> result = new HashMap<>();
            
            // Floyd-Warshall 优化算法
            for(int k = 0; k < n; k++) {
                for(int i = 0; i < n; i++) {
                    if(i != k && !Double.isInfinite(adjacencyMatrix[i][k])) {
                        for(int j = 0; j < n; j++) {
                            if(j != k && j != i && !Double.isInfinite(adjacencyMatrix[k][j])) {
                                double newDist = adjacencyMatrix[i][k] + adjacencyMatrix[k][j];
                                if(newDist < adjacencyMatrix[i][j]) {
                                    adjacencyMatrix[i][j] = newDist;
                                    String key = i + "-" + j;
                                    result.put(key, newDist);
                                }
                            }
                        }
                    }
                }
            }
            return result;
        } catch(Exception e) {
            log.error("计算多源最短路径时发生错误: {}", e.getMessage());
            return new HashMap<>();  
        }
    }

    /**
     * 检验图的强连通性
     * @return true 如果图强连通,false否则
     */
    public static boolean isStronglyConnected(double[][] adjacencyMatrix) {
        try {
            List<Set<Integer>> components = checkConnectivity(adjacencyMatrix);
            return components.size() == 1;
        } catch(Exception e) {
            log.error("检查图强连通性时发生错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 计算最小生成树
     * @return 总权重和最小生成树的边集
     */  
    public static MinSpanningTreeResult calculateMinSpanningTree(double[][] adjacencyMatrix) {
        try {
            int n = adjacencyMatrix.length;
            List<Edge> edges = new ArrayList<>();
            
            // 收集所有边
            for(int i = 0; i < n; i++) {
                for(int j = i + 1; j < n; j++) {
                    if(adjacencyMatrix[i][j] > 0) {
                        edges.add(new Edge(i, j, adjacencyMatrix[i][j]));
                    }
                }
            }
            
            // Kruskal算法
            edges.sort(Comparator.comparingDouble(e -> e.weight));
            DisjointSet ds = new DisjointSet(n);
            List<Edge> mstEdges = new ArrayList<>();
            double totalWeight = 0;
            
            for(Edge e : edges) {
                if(ds.find(e.start) != ds.find(e.end)) {
                    ds.union(e.start, e.end);
                    mstEdges.add(e);
                    totalWeight += e.weight;
                }
            }
            
            return new MinSpanningTreeResult(totalWeight, mstEdges);
        } catch(Exception e) {
            log.error("计算最小生成树时发生错误: {}", e.getMessage());
            return new MinSpanningTreeResult(0, new ArrayList<>());
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
}
