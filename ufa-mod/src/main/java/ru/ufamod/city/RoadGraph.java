package ru.ufamod.city;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * The street network of the map. An edge can be driven in both directions unless it is one-way;
 * a direction of an edge is a "half-edge": {@code edge * 2} forwards, {@code edge * 2 + 1} backwards.
 */
public final class RoadGraph {
    public final int[] x, z;
    public final boolean[] signal;
    private final int[] ea, eb;
    private final float[] width;
    private final boolean[] oneway;
    private final int[][] out;
    private final Map<Long, List<Integer>> grid = new HashMap<>();

    RoadGraph(JsonObject o) {
        JsonArray n = o.getAsJsonArray("n"), e = o.getAsJsonArray("e");
        int nn = n.size() / 3, ne = e.size() / 4;
        x = new int[nn];
        z = new int[nn];
        signal = new boolean[nn];
        for (int i = 0; i < nn; i++) {
            x[i] = n.get(i * 3).getAsInt();
            z[i] = n.get(i * 3 + 1).getAsInt();
            signal[i] = n.get(i * 3 + 2).getAsInt() != 0;
            grid.computeIfAbsent(cell(x[i], z[i]), k -> new ArrayList<>()).add(i);
        }
        ea = new int[ne];
        eb = new int[ne];
        width = new float[ne];
        oneway = new boolean[ne];
        List<List<Integer>> lists = new ArrayList<>();
        for (int i = 0; i < nn; i++) lists.add(new ArrayList<>());
        for (int i = 0; i < ne; i++) {
            ea[i] = e.get(i * 4).getAsInt();
            eb[i] = e.get(i * 4 + 1).getAsInt();
            width[i] = e.get(i * 4 + 2).getAsInt() / 10f;
            oneway[i] = e.get(i * 4 + 3).getAsInt() != 0;
            lists.get(ea[i]).add(i * 2);
            if (!oneway[i]) lists.get(eb[i]).add(i * 2 + 1);
        }
        out = new int[nn][];
        for (int i = 0; i < nn; i++) out[i] = lists.get(i).stream().mapToInt(Integer::intValue).toArray();
    }

    private static long cell(int x, int z) {
        return ((long) (x >> 6) << 32) ^ ((z >> 6) & 0xffffffffL);
    }

    public int size() {
        return x.length;
    }

    public int from(int he) {
        return (he & 1) == 0 ? ea[he >> 1] : eb[he >> 1];
    }

    public int to(int he) {
        return (he & 1) == 0 ? eb[he >> 1] : ea[he >> 1];
    }

    public float width(int he) {
        return width[he >> 1];
    }

    public boolean oneway(int he) {
        return oneway[he >> 1];
    }

    public double length(int he) {
        int a = from(he), b = to(he);
        return Math.hypot(x[b] - x[a], z[b] - z[a]);
    }

    public int[] out(int node) {
        return out[node];
    }

    /** The nearest node that has a way on, or -1. */
    public int nearest(double px, double pz, double maxDist) {
        int best = -1;
        double bd = maxDist * maxDist;
        int r = (int) Math.ceil(maxDist / 64.0);
        int cx = (int) Math.floor(px) >> 6, cz = (int) Math.floor(pz) >> 6;
        for (int gx = cx - r; gx <= cx + r; gx++)
            for (int gz = cz - r; gz <= cz + r; gz++) {
                List<Integer> l = grid.get(((long) gx << 32) ^ (gz & 0xffffffffL));
                if (l == null) continue;
                for (int i : l) {
                    if (out[i].length == 0) continue;
                    double d = (x[i] - px) * (x[i] - px) + (z[i] - pz) * (z[i] - pz);
                    if (d < bd) {
                        bd = d;
                        best = i;
                    }
                }
            }
        return best;
    }

    /** A random node between minDist and maxDist from a point, or -1. */
    public int randomNear(double px, double pz, double minDist, double maxDist, Random rnd) {
        int r = (int) Math.ceil(maxDist / 64.0);
        int cx = (int) Math.floor(px) >> 6, cz = (int) Math.floor(pz) >> 6;
        int found = -1, seen = 0;
        for (int gx = cx - r; gx <= cx + r; gx++)
            for (int gz = cz - r; gz <= cz + r; gz++) {
                List<Integer> l = grid.get(((long) gx << 32) ^ (gz & 0xffffffffL));
                if (l == null) continue;
                for (int i : l) {
                    if (out[i].length == 0) continue;
                    double d = Math.hypot(x[i] - px, z[i] - pz);
                    if (d < minDist || d > maxDist) continue;
                    if (rnd.nextInt(++seen) == 0) found = i;
                }
            }
        return found;
    }

    /** The shortest way as a list of half-edges, or null when there is none. */
    public int[] route(int start, int goal) {
        if (start < 0 || goal < 0) return null;
        double[] dist = new double[x.length];
        int[] via = new int[x.length];
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(via, -1);
        dist[start] = 0;
        PriorityQueue<double[]> q = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        q.add(new double[]{0, start});
        while (!q.isEmpty()) {
            double[] top = q.poll();
            int u = (int) top[1];
            if (top[0] > dist[u]) continue;
            if (u == goal) break;
            for (int he : out[u]) {
                int v = to(he);
                // wide streets are a little "shorter": the bus keeps to the main roads
                double nd = dist[u] + length(he) * (width(he) >= 10 ? 0.8 : 1.0);
                if (nd < dist[v]) {
                    dist[v] = nd;
                    via[v] = he;
                    q.add(new double[]{nd, v});
                }
            }
        }
        if (via[goal] < 0) return null;
        List<Integer> path = new ArrayList<>();
        for (int v = goal; v != start; v = from(via[v])) path.add(via[v]);
        int[] res = new int[path.size()];
        for (int i = 0; i < res.length; i++) res[i] = path.get(res.length - 1 - i);
        return res;
    }
}
