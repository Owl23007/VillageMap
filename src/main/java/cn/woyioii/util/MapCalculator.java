package cn.woyioii.util;

import cn.woyioii.model.Village;

import java.util.*;

public class MapCalculator {
    // 工具类私有构造函数
    private MapCalculator() {

    }

    // 计算两个村庄之间的距离
    public static double calculateDistance(Village village1, Village village2) {
        int x1 = village1.getLocateX();
        int y1 = village1.getLocateY();
        int x2 = village2.getLocateX();
        int y2 = village2.getLocateY();
        
        return calculateDistance( x1,  y1,  x2,  y2);
    }

    // 计算两个坐标之间的距离
    public static double calculateDistance(int x1, int y1, int x2, int y2) {
        // 除以十并保留一位小数
        double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y1 - y2, 2));
        return Math.round(distance / 10.0 * 10.0) / 10.0;
    }

    /**
     * 检查村庄连通性
     * @param adjacencyMatrix 邻接矩阵
     * @return 如果所有村庄都连通返回true，否则返回包含未连通村庄组的列表
     */
    public static List<Set<Integer>> checkConnectivity(double[][] adjacencyMatrix) {
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
    }

        /**
         * 生成需添加的路径
         * @param adjacencyMatrix 邻接矩阵
         * @return 返回需要高亮显示新增的边
         */
        public static List<int[]> addNewRoadToConnect(double[][] adjacencyMatrix, List<Village> villages) {
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

            // 初始化UnionFind并合并原图已存在的边
            UnionFind uf = new UnionFind(n);
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (adjacencyMatrix[i][j] != 0) {
                        uf.union(i, j);
                    }
                }
            }

            List<int[]> newRoads = new ArrayList<>();

            // Kruskal算法选择需要添加的边
            for (Edge edge : edges) {
                int rootStart = uf.find(edge.start);
                int rootEnd = uf.find(edge.end);
                if (rootStart != rootEnd) {
                    newRoads.add(new int[]{edge.start+1, edge.end+1});
                    uf.union(edge.start, edge.end);
                }
            }

            return newRoads;
        }

        // 辅助类：并查集
        static class UnionFind {
            private int[] parent;

            public UnionFind(int size) {
                parent = new int[size];
                for (int i = 0; i < size; i++) {
                    parent[i] = i; // 初始化每个节点的父节点为自己
                }
            }

            // 查找根节点，并进行路径压缩
            public int find(int x) {
                if (parent[x] != x) {
                    parent[x] = find(parent[x]); // 路径压缩
                }
                return parent[x];
            }

            // 合并两个集合
            public void union(int x, int y) {
                int rootX = find(x);
                int rootY = find(y);
                if (rootX != rootY) {
                    parent[rootY] = rootX; // 将rootY的父节点指向rootX
                }
            }
        }

        // 辅助类：边结构
        static class Edge {
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
     * 查找所有点对最短路径（Floyd-Warshall算法）
     * @param adjacencyMatrix 邻接矩阵
     * @return 距离矩阵
     */
    public static double[][] findAllPairsShortestPaths(double[][] adjacencyMatrix) {
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
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                    }
                }
            }
        }
        
        return dist;
    }

    /**
     * 查找所有点对最短路径，并返回路径信息
     * @param adjacencyMatrix 邻接矩阵
     * @return 路径Map，key为目标顶点索引，value为路径顶点列表
     */
    public static Map<Integer, List<Integer>> findAllPairsShortestPathsWithRoute(double[][] adjacencyMatrix) {
        int n = adjacencyMatrix.length;
        double[][] dist = new double[n][n];
        int[][] next = new int[n][n];  // 用于重建路径
        
        // 初始化
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    dist[i][j] = 0;
                } else if (adjacencyMatrix[i][j] > 0) {
                    dist[i][j] = adjacencyMatrix[i][j];
                    next[i][j] = j;  // 直接相连的情况
                } else {
                    dist[i][j] = Double.POSITIVE_INFINITY;
                    next[i][j] = -1;
                }
            }
        }
        
        // Floyd-Warshall算法
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                        next[i][j] = next[i][k];  // 更新路径
                    }
                }
            }
        }
        
        // 重建所有路径
        Map<Integer, List<Integer>> allPaths = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (!Double.isInfinite(dist[0][i])) {
                List<Integer> path = new ArrayList<>();
                path.add(0);  // 添加起点
                int current = 0;
                while (current != i) {
                    current = next[current][i];
                    path.add(current);
                }
                allPaths.put(i, path);
            }
        }
        
        return allPaths;
    }

    /**
     * 计算经过所有点的最短路径
     */
    public static List<Integer> findOptimalRoute(double[][] adjacencyMatrix, int startVertex) {
        int n = adjacencyMatrix.length;
        int endMask = (1 << n) - 1;  // 所有点都访问过的状态
        double[][] dp = new double[1 << n][n];  // dp[mask][last] 表示访问了mask中的点，且以last结尾的最短路径长度
        int[][] parent = new int[1 << n][n];    // 用于重建路径
        
        // 初始化dp数组
        for (double[] row : dp) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        // 初始状态：只访问起点
        dp[1 << startVertex][startVertex] = 0;
        
        // 遍历所有可能的状态
        for (int mask = 0; mask < (1 << n); mask++) {
            for (int last = 0; last < n; last++) {
                if ((mask & (1 << last)) == 0) continue;  // last不在当前路径中
                
                // 尝试扩展到下一个点
                for (int next = 0; next < n; next++) {
                    if ((mask & (1 << next)) != 0) continue;  // next已经在路径中
                    
                    int nextMask = mask | (1 << next);
                    double newDist = dp[mask][last] + adjacencyMatrix[last][next];
                    
                    if (newDist < dp[nextMask][next]) {
                        dp[nextMask][next] = newDist;
                        parent[nextMask][next] = last;
                    }
                }
            }
        }
        
        // 找到最短路径的终点
        double minDist = Double.POSITIVE_INFINITY;
        int lastVertex = -1;
        for (int i = 0; i < n; i++) {
            if (dp[endMask][i] < minDist) {
                minDist = dp[endMask][i];
                lastVertex = i;
            }
        }
        
        // 重建路径
        List<Integer> path = new ArrayList<>();
        if (lastVertex != -1) {
            int mask = endMask;
            int curr = lastVertex;
            while (mask != 0) {
                path.add(0, curr + 1);  // +1 因为村庄ID从1开始
                int prev = parent[mask][curr];
                mask &= ~(1 << curr);
                curr = prev;
            }
        }
        
        return path;
    }

    /**
     * 计算经过所有点并返回起点的最短回路（旅行商问题 - 回环）
     */
    public static List<Integer> findOptimalRoundTrip(double[][] adjacencyMatrix, int startVertex) {
        //todo
        return null;
    }

    // 辅助类：边

    // 辅助类：并查集
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
}
