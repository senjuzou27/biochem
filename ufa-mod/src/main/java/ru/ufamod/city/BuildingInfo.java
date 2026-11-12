package ru.ufamod.city;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** One building of the map as exported by tools/osm_to_city.py. */
public final class BuildingInfo {
    public record Entrance(int x, int z, String kind, String ref) {}

    public record Shop(int x, int z, String kind, String name) {}

    public record Poi(int x, int z, String kind, String name, long node) {}

    public final int index;
    public final long osm;
    public final int minX, minZ, maxX, maxZ;
    public final int baseY;
    public final String type, part, name, street, number, roof, material, colour, amenity, shop, office, tourism, religion, historic;
    public final int levels; // 0 = unknown
    public final int minLevel, roofLevels, area;
    public final List<Entrance> entrances = new ArrayList<>();
    public final List<Shop> shops = new ArrayList<>();
    public final List<Poi> pois = new ArrayList<>();
    public final List<Shop> plaques = new ArrayList<>();

    public BuildingInfo(JsonObject o) {
        index = o.get("i").getAsInt();
        osm = o.get("osm").getAsLong();
        JsonArray bb = o.getAsJsonArray("bb");
        minX = bb.get(0).getAsInt();
        minZ = bb.get(1).getAsInt();
        maxX = bb.get(2).getAsInt();
        maxZ = bb.get(3).getAsInt();
        baseY = o.get("y").getAsInt();
        type = str(o, "type");
        part = str(o, "part");
        name = str(o, "name");
        street = str(o, "street");
        number = str(o, "num");
        roof = str(o, "roof");
        material = str(o, "mat");
        colour = str(o, "col");
        amenity = str(o, "amenity");
        shop = str(o, "shop");
        office = str(o, "office");
        tourism = str(o, "tourism");
        religion = str(o, "religion");
        historic = str(o, "historic");
        levels = o.has("lv") ? o.get("lv").getAsInt() : 0;
        minLevel = o.has("minlv") ? o.get("minlv").getAsInt() : 0;
        roofLevels = o.has("rlv") ? o.get("rlv").getAsInt() : 0;
        area = o.has("area") ? o.get("area").getAsInt() : 0;
        if (o.has("ent"))
            for (JsonElement e : o.getAsJsonArray("ent")) {
                JsonArray a = e.getAsJsonArray();
                entrances.add(new Entrance(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsString(), a.size() > 3 ? a.get(3).getAsString() : ""));
            }
        if (o.has("shops"))
            for (JsonElement e : o.getAsJsonArray("shops")) {
                JsonArray a = e.getAsJsonArray();
                shops.add(new Shop(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsString(), a.get(3).getAsString()));
            }
        if (o.has("plq"))
            for (JsonElement e : o.getAsJsonArray("plq")) {
                JsonArray a = e.getAsJsonArray();
                plaques.add(new Shop(a.get(0).getAsInt(), a.get(1).getAsInt(), "plaque", a.get(2).getAsString()));
            }
        if (o.has("poi"))
            for (JsonElement e : o.getAsJsonArray("poi")) {
                JsonArray a = e.getAsJsonArray();
                pois.add(new Poi(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsString(), a.get(3).getAsString(), a.get(4).getAsLong()));
            }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsString() : "";
    }

    public String address() {
        if (street.isEmpty() || number.isEmpty()) return "";
        return shortStreet(street) + ", " + number;
    }

    public static String shortStreet(String s) {
        return s.replace("улица ", "ул. ").replace(" улица", " ул.").replace("проспект ", "пр. ").replace("площадь", "пл.")
                .replace("переулок ", "пер. ").replace("бульвар ", "б-р ");
    }

    /** A stable pseudo-random number for this building. */
    public long hash() {
        long h = osm * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return h;
    }
}
