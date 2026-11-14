package ru.ufamod.city;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.level.ChunkPos;
import ru.ufamod.UfaMod;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** The map of central Ufa: loaded once from the mod resources, tiles are decompressed on demand. */
public final class CityData {
    private static volatile CityData instance;

    public static CityData get() {
        CityData c = instance;
        if (c == null) {
            synchronized (CityData.class) {
                c = instance;
                if (c == null) instance = c = new CityData();
            }
        }
        return c;
    }

    public final int x0, z0, width, depth, waterY, defaultY;
    public final double angle;
    public final List<BuildingInfo> buildings = new ArrayList<>();
    private final Map<Long, List<MapPoint>> pointsByChunk = new HashMap<>();
    private final Map<Long, List<BuildingInfo>> buildingsByChunk = new HashMap<>();
    public final Map<String, Integer> roles = new HashMap<>();
    public final Map<String, int[]> spots = new HashMap<>();
    /** The street network for the bus and the traffic. */
    public final RoadGraph graph;
    private final Tile outside;
    private final LoadingCache<Long, Tile> tiles;

    private CityData() {
        long t0 = System.currentTimeMillis();
        JsonObject meta;
        try (InputStream in = open("city.json.gz")) {
            meta = JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read the city of Ufa from the mod resources", e);
        }
        x0 = meta.get("x0").getAsInt();
        z0 = meta.get("z0").getAsInt();
        width = meta.get("w").getAsInt();
        depth = meta.get("h").getAsInt();
        waterY = meta.get("waterY").getAsInt();
        defaultY = meta.get("defaultY").getAsInt();
        angle = meta.get("angle").getAsDouble();
        for (JsonElement e : meta.getAsJsonArray("buildings")) buildings.add(new BuildingInfo(e.getAsJsonObject()));
        for (BuildingInfo b : buildings) {
            int m = 8;
            for (int cx = (b.minX - m) >> 4; cx <= (b.maxX + m) >> 4; cx++)
                for (int cz = (b.minZ - m) >> 4; cz <= (b.maxZ + m) >> 4; cz++)
                    buildingsByChunk.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>()).add(b);
        }
        for (JsonElement e : meta.getAsJsonArray("points")) {
            JsonObject o = e.getAsJsonObject();
            float dx = 0, dz = 0;
            if (o.has("d")) {
                JsonArray d = o.getAsJsonArray("d");
                dx = d.get(0).getAsFloat();
                dz = d.get(1).getAsFloat();
            }
            MapPoint p = new MapPoint(MapPoint.Kind.byId(o.get("k").getAsInt()), o.get("x").getAsInt(), o.get("z").getAsInt(),
                    o.has("n") ? o.get("n").getAsString() : "", o.has("m") ? o.get("m").getAsString() : null,
                    o.has("s") ? o.get("s").getAsString() : null, dx, dz);
            int r = p.kind().radius();
            for (int cx = (p.x() - r) >> 4; cx <= (p.x() + r) >> 4; cx++)
                for (int cz = (p.z() - r) >> 4; cz <= (p.z() + r) >> 4; cz++)
                    pointsByChunk.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>()).add(p);
        }
        graph = new RoadGraph(meta.getAsJsonObject("graph"));
        JsonObject places = meta.getAsJsonObject("places");
        for (Map.Entry<String, JsonElement> e : places.getAsJsonObject("roles").entrySet()) roles.put(e.getKey(), e.getValue().getAsInt());
        for (Map.Entry<String, JsonElement> e : places.getAsJsonObject("spots").entrySet()) {
            JsonArray a = e.getValue().getAsJsonArray();
            spots.put(e.getKey(), new int[]{a.get(0).getAsInt(), a.get(1).getAsInt()});
        }
        outside = Tile.flat(defaultY);
        tiles = CacheBuilder.newBuilder().maximumSize(160).build(new CacheLoader<>() {
            @Override
            public Tile load(Long key) {
                return loadTile((int) (key >> 32), (int) (long) key);
            }
        });
        UfaMod.LOGGER.info("City of Ufa loaded: {} buildings, {} chunks with objects, {} ms", buildings.size(), pointsByChunk.size(), System.currentTimeMillis() - t0);
    }

    private static InputStream open(String name) {
        InputStream in = CityData.class.getResourceAsStream("/ufamod_city/" + name);
        if (in == null) throw new IllegalStateException("Missing resource ufamod_city/" + name);
        return in;
    }

    private Tile loadTile(int tx, int tz) {
        InputStream raw = CityData.class.getResourceAsStream("/ufamod_city/tiles/" + tx + "_" + tz + ".bin.gz");
        if (raw == null) return outside;
        int n = Tile.SIZE * Tile.SIZE;
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
            byte[] all = in.readAllBytes();
            ByteBuffer buf = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
            short[] h = new short[n];
            buf.asShortBuffer().get(h);
            buf.position(2 * n);
            byte[] g = new byte[n], m = new byte[n], f = new byte[n];
            buf.get(g);
            buf.get(m);
            buf.get(f);
            short[] b = new short[n];
            buf.asShortBuffer().get(b);
            return new Tile(h, g, m, f, b);
        } catch (Exception e) {
            UfaMod.LOGGER.error("Broken city tile {} {}", tx, tz, e);
            return outside;
        }
    }

    public Tile tile(int x, int z) {
        return tiles.getUnchecked(((long) Math.floorDiv(x, Tile.SIZE) << 32) | (Math.floorDiv(z, Tile.SIZE) & 0xffffffffL));
    }

    private static int idx(int x, int z) {
        return Math.floorMod(z, Tile.SIZE) * Tile.SIZE + Math.floorMod(x, Tile.SIZE);
    }

    public int height(int x, int z) {
        return tile(x, z).height[idx(x, z)];
    }

    public int ground(int x, int z) {
        return tile(x, z).ground[idx(x, z)] & 0xff;
    }

    public int mark(int x, int z) {
        return tile(x, z).mark[idx(x, z)] & 0xff;
    }

    public int feat(int x, int z) {
        return tile(x, z).feat[idx(x, z)] & 0xff;
    }

    public int building(int x, int z) {
        return tile(x, z).building[idx(x, z)] & 0xffff;
    }

    public BuildingInfo buildingInfo(int index) {
        return index >= 1 && index <= buildings.size() ? buildings.get(index - 1) : null;
    }

    public BuildingInfo role(String role) {
        Integer i = roles.get(role);
        return i == null ? null : buildingInfo(i);
    }

    public List<MapPoint> points(int chunkX, int chunkZ) {
        return pointsByChunk.getOrDefault(ChunkPos.asLong(chunkX, chunkZ), Collections.emptyList());
    }

    public List<BuildingInfo> buildings(int chunkX, int chunkZ) {
        return buildingsByChunk.getOrDefault(ChunkPos.asLong(chunkX, chunkZ), Collections.emptyList());
    }

    public boolean inside(int x, int z) {
        return x >= x0 && z >= z0 && x < x0 + width && z < z0 + depth;
    }
}
