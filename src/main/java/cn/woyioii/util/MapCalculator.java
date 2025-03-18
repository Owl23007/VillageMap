package cn.woyioii.util;

import cn.woyioii.model.Village;

import java.util.*;
import java.util.stream.Collectors;

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
        double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        return Math.round(distance / 10.0 * 10.0) / 10.0;
    }

    /**
     * 检查村庄连通性
     * @param adjacencyMatrix 邻接矩阵
     * @return 如果所有村庄都连通返回true，否则返回包含未连通村庄组的列表
     */
    public static List<Set<Integer>> checkConnectivity(double[][] adjacencyMatrix) {
        int n = adjacencyMatrix.length;
        DisjointSet ds = new DisjointSet(n);
        
        // 使用并查集检查连通性
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (adjacencyMatrix[i][j] > 0) {
                    ds.union(i, j);
                }
            }
        }
        
        // 获取所有连通分量
        Map<Integer, Set<Integer>> components = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = ds.find(i);
            components.computeIfAbsent(root, k -> new HashSet<>()).add(i);
        }
        
        return new ArrayList<>(components.values());
    }

    /**
     * 生成最小生成树（Kruskal算法）
     * @param adjacencyMatrix 邻接矩阵
     * @return 最小生成树的边集合，每个边用int[]{from, to}表示
     */
    public static List<int[]> generateMinimumSpanningTree(double[][] adjacencyMatrix) {
        int n = adjacencyMatrix.length;
        List<Edge> edges = new ArrayList<>();
        
        // 收集所有边
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (adjacencyMatrix[i][j] > 0) {
                    edges.add(new Edge(i, j, adjacencyMatrix[i][j]));
                }
            }
        }
        
        // 按权重排序
        edges.sort(Comparator.comparingDouble(e -> e.weight));
        
        // Kruskal算法
        DisjointSet ds = new DisjointSet(n);
        List<int[]> mst = new ArrayList<>();
        
        for (Edge edge : edges) {
            if (ds.find(edge.from) != ds.find(edge.to)) {
                ds.union(edge.from, edge.to);
                mst.add(new int[]{edge.from, edge.to});
            }
        }
        
        return mst;
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
     * 计算经过所有点的最短路径（旅行商问题 - 非回环）
     */
    public static List<Integer> findOptimalRoute(double[][] adjacencyMatrix, int startVertex) {
    //todo
        return null;
    }

    /**
     * 计算经过所有点并返回起点的最短回路（旅行商问题 - 回环）
     */
    public static List<Integer> findOptimalRoundTrip(double[][] adjacencyMatrix, int startVertex) {
        //todo
        return null;
    }

    // 辅助类：边
    private static class Edge {
        int from, to;
        double weight;
        
        Edge(int from, int to, double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
    }

    // 辅助类：并查集
    private static class DisjointSet {
        private final int[] parent;
        private final int[] rank;
        
        DisjointSet(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }
        
        int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // 路径压缩
            }
            return parent[x];
        }
        
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
