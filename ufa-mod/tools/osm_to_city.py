#!/usr/bin/env python3
"""Turns OpenStreetMap data and terrain tiles of central Ufa into the data the mod builds the city from.

    python3 tools/fetch_osm.py  work        # download (not committed)
    python3 tools/osm_to_city.py work       # writes src/main/resources/ufamod_city/

Scale is 1:1 (one block = one metre). The whole map is rotated by ANGLE degrees so that the historic
street grid of the centre (ul. Lenina, ul. Pushkina ...) runs along the block axes; north in the game
is therefore 15 degrees off the real north.

Output
  ufamod_city/city.json.gz       metadata, buildings, point objects, story places
  ufamod_city/tiles/X_Z.bin.gz   256x256 tiles; per cell: int16 terrain y, uint8 ground, uint8 marking,
                                 uint8 line feature, uint16 building index (0 = none)
Map data (c) OpenStreetMap contributors (ODbL).
"""
import glob
import gzip
import json
import math
import os
import random
import sys
import xml.etree.ElementTree as ET

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

LAT0, LON0 = 54.7200, 55.9440
KX, KZ = 64475.0, 111318.0
ANGLE = math.radians(15.4)
COS, SIN = math.cos(ANGLE), math.sin(ANGLE)
TILE = 256
BBOX = (54.7060, 54.7380, 55.9180, 55.9895)  # lat min, lat max, lon min, lon max (as in fetch_osm.py)
WATER_Y = 62          # surface of the Belaya
ELEV_RIVER = 85.0     # metres above sea level that map to WATER_Y

# ground codes (keep in sync with ru.ufamod.city.Ground)
VOID, GRASS, ROAD, SIDEWALK, PLAZA, PATH, PARKING, WATER, SAND, PITCH, PLAYGROUND, FLOWERBED, FOREST, DIRT, \
    YARD, STEPS, TRACK, GARAGES, FOUNTAIN, BRIDGE = range(20)
# marking codes
M_NONE, M_LINE, M_ZEBRA, M_RAIL, M_CURB, M_PARKING, M_STOP = range(7)
# line features
F_NONE, F_FENCE, F_WALL, F_HEDGE, F_RETAINING, F_WOODFENCE, F_RAIL_GUARD = range(7)

# point kinds (keep in sync with ru.ufamod.city.PointKind)
P_KINDS = ["tree", "bench", "lamp", "bus_stop", "signals", "fountain", "statue", "bust", "stele", "memorial",
           "kiosk", "street_sign", "waste", "bicycle", "artwork", "conifer", "birch", "flowers"]


def box_blur(a, r):
    k = 2 * r + 1
    p = np.pad(a, r, mode="edge")
    c = np.cumsum(np.cumsum(p, 0), 1)
    c = np.pad(c, ((1, 0), (1, 0)))
    return (c[k:, k:] - c[:-k, k:] - c[k:, :-k] + c[:-k, :-k]) / (k * k)


def proj(lat, lon):
    x = (lon - LON0) * KX
    z = (LAT0 - lat) * KZ
    return x * COS + z * SIN, -x * SIN + z * COS


def unproj(x, z):
    xu = x * COS - z * SIN
    zu = x * SIN + z * COS
    return LAT0 - zu / KZ, LON0 + xu / KX


def load_osm(tiles_dir):
    nodes, ways, rels = {}, {}, {}
    for f in sorted(glob.glob(os.path.join(tiles_dir, "*.xml"))):
        for el in ET.parse(f).getroot():
            tags = {t.get("k"): t.get("v") for t in el.findall("tag")}
            if el.tag == "node":
                nodes[int(el.get("id"))] = (float(el.get("lat")), float(el.get("lon")), tags)
            elif el.tag == "way":
                ways[int(el.get("id"))] = ([int(n.get("ref")) for n in el.findall("nd")], tags)
            elif el.tag == "relation":
                rels[int(el.get("id"))] = ([(m.get("type"), int(m.get("ref")), m.get("role")) for m in el.findall("member")], tags)
    return nodes, ways, rels


class City:
    def __init__(self, work):
        self.nodes, self.ways, self.rels = load_osm(os.path.join(work, "tiles"))
        print("osm", len(self.nodes), len(self.ways), len(self.rels))
        self.xy = {nid: proj(la, lo) for nid, (la, lo, _) in self.nodes.items()}
        # grid bounds: rotated bounding box of the downloaded area, aligned to whole tiles
        # the downloaded area (ways reach out with nodes far beyond it, so the nodes cannot be used)
        lats = [BBOX[0], BBOX[1]]
        lons = [BBOX[2], BBOX[3]]
        corners = [proj(a, b) for a in lats for b in lons]
        self.x0 = int(math.floor(min(c[0] for c in corners) / TILE)) * TILE
        self.z0 = int(math.floor(min(c[1] for c in corners) / TILE)) * TILE
        self.x1 = int(math.ceil(max(c[0] for c in corners) / TILE)) * TILE
        self.z1 = int(math.ceil(max(c[1] for c in corners) / TILE)) * TILE
        self.w, self.h = self.x1 - self.x0, self.z1 - self.z0
        print("grid", self.x0, self.z0, self.w, self.h)
        self.bbox = (min(lats), max(lats), min(lons), max(lons))
        self.work = work

    # ------------------------------------------------------------------ helpers
    def pts(self, nds):
        return [(self.xy[n][0] - self.x0, self.xy[n][1] - self.z0) for n in nds if n in self.xy]

    def ring_of(self, wid):
        nds, _ = self.ways[wid]
        p = self.pts(nds)
        return p if len(p) >= 3 else None

    def multipolygon(self, rid):
        """Assembles outer and inner rings of a multipolygon relation."""
        members, _ = self.rels[rid]
        out = {"outer": [], "inner": []}
        for role in ("outer", "inner"):
            segs = [list(self.ways[ref][0]) for typ, ref, r in members if typ == "way" and (r or "outer") == role and ref in self.ways]
            rings = []
            while segs:
                ring = segs.pop(0)
                changed = True
                while ring[0] != ring[-1] and changed:
                    changed = False
                    for i, s in enumerate(segs):
                        if s[0] == ring[-1]:
                            ring += s[1:]
                        elif s[-1] == ring[-1]:
                            ring += s[::-1][1:]
                        elif s[-1] == ring[0]:
                            ring = s[:-1] + ring
                        elif s[0] == ring[0]:
                            ring = s[::-1][:-1] + ring
                        else:
                            continue
                        segs.pop(i)
                        changed = True
                        break
                p = self.pts(ring)
                # members outside the downloaded area are missing: keep closed rings only
                if len(p) >= 3 and ring[0] == ring[-1]:
                    rings.append(p)
            out[role] = rings
        return out

    def areas(self, pred):
        """All closed ways and multipolygons whose tags match pred: yields (rings_outer, rings_inner, tags, id)."""
        for wid, (nds, t) in self.ways.items():
            if len(nds) > 3 and nds[0] == nds[-1] and pred(t):
                r = self.ring_of(wid)
                if r:
                    yield [r], [], t, wid
        for rid, (mem, t) in self.rels.items():
            if t.get("type") == "multipolygon" and pred(t):
                mp = self.multipolygon(rid)
                if mp["outer"]:
                    yield mp["outer"], mp["inner"], t, -rid

    # ------------------------------------------------------------------ terrain
    def terrain(self):
        dem = os.path.join(self.work, "dem")
        files = glob.glob(os.path.join(dem, "*.png"))
        txs = sorted({int(os.path.basename(f).split("_")[0]) for f in files})
        tys = sorted({int(os.path.basename(f).split("_")[1].split(".")[0]) for f in files})
        mosaic = np.zeros(((tys[-1] - tys[0] + 1) * 256, (txs[-1] - txs[0] + 1) * 256), np.float32)
        for f in files:
            tx = int(os.path.basename(f).split("_")[0]); ty = int(os.path.basename(f).split("_")[1].split(".")[0])
            a = np.asarray(Image.open(f).convert("RGB"), np.float32)
            mosaic[(ty - tys[0]) * 256:(ty - tys[0] + 1) * 256, (tx - txs[0]) * 256:(tx - txs[0] + 1) * 256] = a[..., 0] * 256 + a[..., 1] + a[..., 2] / 256 - 32768
        z = 15
        n = 2 ** z
        zz, xx = np.mgrid[0:self.h, 0:self.w].astype(np.float64)
        xs = xx + self.x0 + 0.5
        zs = zz + self.z0 + 0.5
        xu = xs * COS - zs * SIN
        zu = xs * SIN + zs * COS
        lat = LAT0 - zu / KZ
        lon = LON0 + xu / KX
        px = ((lon + 180) / 360 * n - txs[0]) * 256
        py = ((1 - np.arcsinh(np.tan(np.radians(lat))) / np.pi) / 2 * n - tys[0]) * 256
        px = np.clip(px - 0.5, 0, mosaic.shape[1] - 2)
        py = np.clip(py - 0.5, 0, mosaic.shape[0] - 2)
        ix, iy = px.astype(np.int32), py.astype(np.int32)
        fx, fy = px - ix, py - iy
        e = (mosaic[iy, ix] * (1 - fx) * (1 - fy) + mosaic[iy, ix + 1] * fx * (1 - fy)
             + mosaic[iy + 1, ix] * (1 - fx) * fy + mosaic[iy + 1, ix + 1] * fx * fy)
        e = np.clip(e, 60, 260)  # a few nodata pixels in the source tiles
        for _ in range(3):  # three box blurs approximate a gaussian
            e = box_blur(e, 5)
        self.elev = e
        print("elevation", float(e.min()), float(e.max()))
        y = np.round(e - ELEV_RIVER + WATER_Y)
        self.elev_raw = e
        return np.clip(y, WATER_Y - 8, 250).astype(np.int16)

    # ------------------------------------------------------------------ main
    def build(self, out_dir):
        W, H = self.w, self.h
        heights = self.terrain()
        g_img = Image.new("L", (W, H), GRASS)
        g = ImageDraw.Draw(g_img)
        m_img = Image.new("L", (W, H), 0)
        m = ImageDraw.Draw(m_img)
        f_img = Image.new("L", (W, H), 0)
        fdraw = ImageDraw.Draw(f_img)
        b_img = Image.new("I", (W, H), 0)
        bdraw = ImageDraw.Draw(b_img)

        def poly(draw, outer, inner, val):
            for r in outer:
                draw.polygon(r, fill=val)
            for r in inner:
                draw.polygon(r, fill=GRASS if draw is g else 0)

        # ---- areas, lowest priority first
        area_rules = [
            (lambda t: t.get("landuse") in ("industrial", "railway"), YARD),
            (lambda t: t.get("landuse") == "garages", GARAGES),
            (lambda t: t.get("landuse") in ("construction", "brownfield"), DIRT),
            (lambda t: t.get("landuse") in ("grass", "meadow", "village_green", "recreation_ground") or t.get("leisure") in ("park", "garden"), GRASS),
            (lambda t: t.get("natural") in ("wood", "scrub") or t.get("landuse") == "forest", FOREST),
            (lambda t: t.get("amenity") == "parking" and t.get("parking") not in ("underground", "multi-storey"), PARKING),
            (lambda t: t.get("leisure") in ("pitch", "sports_centre") and t.get("surface") not in ("grass",), PITCH),
            (lambda t: t.get("leisure") == "track", TRACK),
            (lambda t: t.get("leisure") == "playground", PLAYGROUND),
            (lambda t: t.get("landuse") == "flowerbed", FLOWERBED),
            (lambda t: t.get("natural") in ("sand", "beach"), SAND),
            (lambda t: (t.get("highway") == "pedestrian" and t.get("area") == "yes") or "area:highway" in t or t.get("place") == "square" or t.get("amenity") == "marketplace", PLAZA),
            (lambda t: (t.get("natural") == "water" or t.get("water")) and t.get("water") not in ("river",), WATER),
            (lambda t: t.get("amenity") == "fountain", FOUNTAIN),
        ]
        for pred, code in area_rules:
            for outer, inner, t, _ in self.areas(pred):
                poly(g, outer, inner, code)

        # ---- the Belaya: everything below the river level near the mapped river centre line
        river_e = []
        for wid, (nds, t) in self.ways.items():
            if t.get("waterway") == "river":
                p = self.pts(nds)
                for (ax, az), (bx, bz) in zip(p, p[1:]):
                    L = max(1.0, math.hypot(bx - ax, bz - az))
                    for k in range(0, int(L), 10):
                        x, z = ax + (bx - ax) * k / L, az + (bz - az) * k / L
                        lat, lon = unproj(x + self.x0, z + self.z0)
                        if 0 <= int(z) < H and 0 <= int(x) < W and BBOX[0] < lat < BBOX[1] and BBOX[2] < lon < BBOX[3]:
                            river_e.append(self.elev[int(z), int(x)])
        if river_e:
            print("river samples", [round(float(np.percentile(river_e, q)), 1) for q in (5, 25, 50, 75, 95)])
        level = float(np.percentile(river_e, 50)) + 1.0 if river_e else ELEV_RIVER
        print("river level", level, "from", len(river_e), "samples")
        self.river_level = level
        heights = np.clip(np.round(self.elev - level + WATER_Y), WATER_Y - 8, 250).astype(np.int16)
        near = Image.new("L", (W, H), 0)
        nd = ImageDraw.Draw(near)
        for wid, (nds, t) in self.ways.items():
            if t.get("waterway") == "river":
                p = self.pts(nds)
                if len(p) >= 2:
                    nd.line(p, fill=255, width=420, joint="curve")
        low = Image.fromarray(((self.elev < level) & (np.asarray(near) > 0)).astype(np.uint8) * 255)
        g_img.paste(WATER, mask=low)

        # ---- linear ways
        def width_of(t, default):
            try:
                return float(t.get("width", "").split()[0].replace(",", "."))
            except (ValueError, IndexError):
                return default

        foot = {"footway": (2.5, SIDEWALK), "path": (1.6, PATH), "cycleway": (2.0, SIDEWALK), "bridleway": (2, PATH),
                "track": (3, PATH), "steps": (2.5, STEPS), "pedestrian": (7, PLAZA), "living_street": (6, ROAD), "corridor": (0, 0)}
        unpaved = ("unpaved", "gravel", "dirt", "ground", "compacted", "fine_gravel", "earth", "grass", "sand", "mud")
        roads = []
        for wid, (nds, t) in self.ways.items():
            hw = t.get("highway")
            if not hw or t.get("area") == "yes" or t.get("tunnel") in ("yes", "building_passage") or t.get("indoor") == "yes":
                continue
            p = self.pts(nds)
            if len(p) < 2:
                continue
            if hw in foot:
                w, code = foot[hw]
                if w == 0:
                    continue
                if code == SIDEWALK and t.get("surface") in unpaved:
                    code = PATH
                if code == PATH and t.get("surface") in ("asphalt", "paving_stones", "concrete", "sett"):
                    code = SIDEWALK
                g.line(p, fill=code, width=max(1, int(round(width_of(t, w)))), joint="curve")
            else:
                roads.append((wid, hw, t, p, nds))
        rank = {"service": 0, "unclassified": 1, "residential": 1, "tertiary": 2, "tertiary_link": 2, "secondary_link": 3,
                "secondary": 3, "primary_link": 4, "primary": 4, "trunk": 5, "trunk_link": 5}
        roads = [r for r in roads if r[1] in rank]
        roads.sort(key=lambda r: rank[r[1]])
        self.road_info = []
        for wid, hw, t, p, nds in roads:
            lanes = None
            try:
                lanes = int(t.get("lanes", "0").split(";")[0])
            except ValueError:
                pass
            oneway = t.get("oneway") in ("yes", "1", "-1")
            if hw == "service":
                w = 4.0 if t.get("service") in ("parking_aisle", "driveway") else 5.0
                lanes = lanes or 1
            else:
                lanes = lanes or {"unclassified": 2, "residential": 2, "tertiary": 2, "tertiary_link": 1, "secondary": 4,
                                  "secondary_link": 1, "primary": 4, "primary_link": 1, "trunk": 6, "trunk_link": 1}[hw]
                w = width_of(t, lanes * 3.5 + (0 if oneway else 0.5))
            sw = 0 if hw == "service" or t.get("sidewalk") in ("no", "none", "separate") else (3 if rank[hw] >= 3 else 2.5)
            self.road_info.append({"id": wid, "hw": hw, "w": w, "lanes": lanes, "oneway": oneway, "p": p, "nds": nds,
                                   "name": t.get("name", ""), "tram": False, "sw": sw})
        # sidewalks first, then all carriageways on top so a sidewalk never cuts a road
        for r in self.road_info:
            if r["sw"] > 0:
                g.line(r["p"], fill=SIDEWALK, width=int(round(r["w"] + 2 * r["sw"])), joint="curve")
        for r in self.road_info:
            g.line(r["p"], fill=ROAD if r["hw"] != "service" else YARD if False else ROAD, width=max(2, int(round(r["w"]))), joint="curve")

        # ---- barriers and walls
        lines = {"fence": F_FENCE, "wall": F_WALL, "hedge": F_HEDGE, "retaining_wall": F_RETAINING, "guard_rail": F_RAIL_GUARD,
                 "city_wall": F_WALL, "handrail": F_RAIL_GUARD}
        for wid, (nds, t) in self.ways.items():
            b = t.get("barrier")
            if b in lines:
                code = lines[b]
                if code == F_FENCE and (t.get("fence_type") in ("wood", "chain_link_wood", "pickets", "split_rail") or t.get("material") == "wood"):
                    code = F_WOODFENCE
                p = self.pts(nds)
                if len(p) >= 2:
                    fdraw.line(p, fill=code, width=1)

        # ---- buildings
        self.buildings = []
        bmeta = []
        for outer, inner, t, oid in self.areas(lambda t: ("building" in t and t["building"] not in ("no", "roof", "construction", "ruins", "collapsed")) or "building:part" in t):
            if t.get("location") == "underground" or t.get("layer", "0").startswith("-"):
                continue
            part = "building:part" in t and "building" not in t
            bmeta.append((1 if part else 0, outer, inner, t, oid))
        bmeta.sort(key=lambda b: b[0])  # outlines first, parts painted over them
        for part, outer, inner, t, oid in bmeta:
            idx = len(self.buildings) + 1
            for r in outer:
                bdraw.polygon(r, fill=idx)
            for r in inner:
                bdraw.polygon(r, fill=0)
            self.buildings.append({"t": t, "osm": oid, "outer": outer, "inner": inner, "part": part})
        print("buildings", len(self.buildings))

        G = np.asarray(g_img, np.uint8).copy()
        M = np.asarray(m_img, np.uint8).copy()
        F = np.asarray(f_img, np.uint8).copy()
        B = np.asarray(b_img, np.int32).astype(np.uint16)
        self.G, self.B = G, B

        # ---- markings, rails, crossings
        self.markings(M, G)
        # curbs: sidewalk cells next to a carriageway
        road = G == ROAD
        side = G == SIDEWALK
        adj = np.zeros_like(road)
        adj[1:, :] |= road[:-1, :]; adj[:-1, :] |= road[1:, :]; adj[:, 1:] |= road[:, :-1]; adj[:, :-1] |= road[:, 1:]
        M[side & adj & (M == 0)] = M_CURB
        # fences never run through roads or buildings
        F[(G == ROAD) | (B > 0)] = 0

        # ---- terrain fixes: water, river banks, flat building sites
        water = (G == WATER)
        Hh = heights.copy()
        Hh[water] = WATER_Y
        low = (~water) & (Hh <= WATER_Y)
        Hh[low] = WATER_Y + 1
        self.flatten_buildings(Hh, B)
        self.water_depth(Hh, water)
        self.H = Hh

        # ---- points
        points = self.points(G, B, Hh)
        print("points", len(points))

        # ---- building records
        brecs = self.building_records(B, Hh)
        places = self.places(brecs)

        # ---- write
        os.makedirs(os.path.join(out_dir, "tiles"), exist_ok=True)
        for f in glob.glob(os.path.join(out_dir, "tiles", "*.bin.gz")):
            os.remove(f)
        n_tiles = 0
        for tz in range(H // TILE):
            for tx in range(W // TILE):
                sl = (slice(tz * TILE, (tz + 1) * TILE), slice(tx * TILE, (tx + 1) * TILE))
                gt, bt = G[sl], B[sl]
                buf = (Hh[sl].astype("<i2").tobytes() + gt.tobytes() + M[sl].tobytes() + F[sl].tobytes() + bt.astype("<u2").tobytes())
                wx, wz = (self.x0 + tx * TILE) // TILE, (self.z0 + tz * TILE) // TILE
                with gzip.open(os.path.join(out_dir, "tiles", f"{wx}_{wz}.bin.gz"), "wb", 9) as fh:
                    fh.write(buf)
                n_tiles += 1
        meta = {
            "version": 1, "tile": TILE, "x0": self.x0, "z0": self.z0, "w": W, "h": H, "waterY": WATER_Y,
            "defaultY": int(np.median(Hh)), "angle": math.degrees(ANGLE),
            "buildings": brecs, "points": points, "places": places,
            "attribution": "Map data (c) OpenStreetMap contributors, ODbL. Elevation: Mapzen/AWS terrain tiles.",
        }
        with gzip.open(os.path.join(out_dir, "city.json.gz"), "wt", encoding="utf-8") as fh:
            json.dump(meta, fh, ensure_ascii=False, separators=(",", ":"))
        print("tiles", n_tiles, "defaultY", meta["defaultY"])
        self.preview(out_dir, G, M, B, Hh, points)

    # ------------------------------------------------------------------ markings
    def markings(self, M, G):
        H, W = M.shape

        def put(x, z, code, only=(ROAD,)):
            ix, iz = int(math.floor(x)), int(math.floor(z))
            if 0 <= ix < W and 0 <= iz < H and G[iz, ix] in only:
                M[iz, ix] = code

        # junctions: nodes shared by two or more roads; markings stop short of them
        count = {}
        for r in self.road_info:
            for n in r["nds"]:
                count[n] = count.get(n, 0) + 1
        junction = {n for n, c in count.items() if c >= 2}
        jpos = [self.xy[n] for n in junction if n in self.xy]
        jgrid = {}
        for (x, z) in jpos:
            jgrid.setdefault((int((x - self.x0) // 16), int((z - self.z0) // 16)), []).append((x - self.x0, z - self.z0))

        def near_junction(x, z, rad):
            gx, gz = int(x // 16), int(z // 16)
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    for (jx, jz) in jgrid.get((gx + dx, gz + dz), ()):
                        if (jx - x) ** 2 + (jz - z) ** 2 < rad * rad:
                            return True
            return False

        for r in self.road_info:
            if r["hw"] == "service" or r["lanes"] < 2 and not r["oneway"]:
                continue
            p, w, lanes = r["p"], r["w"], r["lanes"]
            lane_w = w / max(1, lanes)
            offsets = []
            if r["oneway"]:
                offsets = [(-w / 2 + lane_w * k, True) for k in range(1, lanes)]
            else:
                offsets = [(0.0, lanes < 4)]
                half = lanes // 2
                for k in range(1, half):
                    offsets += [(lane_w * k, True), (-lane_w * k, True)]
            s_total = 0.0
            for (ax, az), (bx, bz) in zip(p, p[1:]):
                L = math.hypot(bx - ax, bz - az)
                if L < 0.01:
                    continue
                ux, uz = (bx - ax) / L, (bz - az) / L
                nx, nz = -uz, ux
                t = 0.0
                while t < L:
                    s = s_total + t
                    cx, cz = ax + ux * t, az + uz * t
                    if not near_junction(cx, cz, w / 2 + 6):
                        for off, dashed in offsets:
                            if not dashed or (s % 6.0) < 3.0:
                                put(cx + nx * off, cz + nz * off, M_LINE)
                    t += 0.5
                s_total += L
        # tram tracks
        for wid, (nds, t) in self.ways.items():
            if t.get("railway") == "tram":
                p = self.pts(nds)
                for (ax, az), (bx, bz) in zip(p, p[1:]):
                    L = math.hypot(bx - ax, bz - az)
                    if L < 0.01:
                        continue
                    ux, uz = (bx - ax) / L, (bz - az) / L
                    nx, nz = -uz, ux
                    t2 = 0.0
                    while t2 < L:
                        for off in (-0.75, 0.75):
                            put(ax + ux * t2 + nx * off, az + uz * t2 + nz * off, M_RAIL, (ROAD, SIDEWALK, PLAZA, GRASS, PATH, YARD, PARKING))
                        t2 += 0.4
        # zebra crossings
        way_of = {}
        for r in self.road_info:
            for i, n in enumerate(r["nds"]):
                way_of.setdefault(n, []).append((r, i))
        for nid, (la, lo, t) in self.nodes.items():
            if t.get("highway") != "crossing" or nid not in way_of:
                continue
            r, i = max(way_of[nid], key=lambda ri: ri[0]["w"])
            nds = r["nds"]
            a = self.xy[nds[max(0, i - 1)]]
            b = self.xy[nds[min(len(nds) - 1, i + 1)]]
            L = math.hypot(b[0] - a[0], b[1] - a[1])
            if L < 0.01:
                continue
            ux, uz = (b[0] - a[0]) / L, (b[1] - a[1]) / L
            nx, nz = -uz, ux
            cx, cz = self.xy[nid][0] - self.x0, self.xy[nid][1] - self.z0
            half = r["w"] / 2 + 0.5
            lat = -half
            while lat <= half:
                if int(math.floor(lat + 100)) % 2 == 0:
                    for al in np.arange(-2.0, 2.01, 0.5):
                        put(cx + ux * al + nx * lat, cz + uz * al + nz * lat, M_ZEBRA)
                lat += 0.5
        # parking bays
        Hh, Ww = M.shape
        park = G == PARKING
        zz, xx = np.nonzero(park)
        sel = (xx % 3 == 0)
        M[zz[sel], xx[sel]] = M_PARKING

    # ------------------------------------------------------------------ terrain helpers
    def flatten_buildings(self, Hh, B):
        idx = B.astype(np.int64)
        n = len(self.buildings) + 1
        self.base = np.zeros(n, np.int32)
        flat = idx.ravel()
        hv = Hh.ravel()
        order = np.argsort(flat, kind="stable")
        fs = flat[order]
        starts = np.searchsorted(fs, np.arange(n))
        ends = np.searchsorted(fs, np.arange(n), side="right")
        for i in range(1, n):
            if ends[i] > starts[i]:
                self.base[i] = int(np.median(hv[order[starts[i]:ends[i]]]))
        inside = idx > 0
        Hh[inside] = self.base[idx[inside]]
        # blend a two-metre ring around each building towards its floor level
        for _ in range(2):
            grown = idx.copy()
            for dz, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                sh = np.roll(idx, (dz, dx), (0, 1))
                grown = np.where((grown == 0) & (sh > 0), sh, grown)
            ring = (idx == 0) & (grown > 0)
            target = self.base[grown[ring]]
            cur = Hh[ring]
            close = np.abs(cur - target) <= 2
            vals = cur.copy()
            vals[close] = target[close]
            Hh[ring] = vals
            idx = np.where(ring, grown, idx)

    def water_depth(self, Hh, water):
        # depth grows with distance from the bank, up to 6 blocks
        d = water.astype(np.int16)
        cur = water.copy()
        for k in range(1, 7):
            er = cur.copy()
            for dz, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                er &= np.roll(cur, (dz, dx), (0, 1))
            cur = er
            d += cur.astype(np.int16)
        self.water_d = d

    # ------------------------------------------------------------------ points
    def points(self, G, B, Hh):
        H, W = G.shape
        rnd = random.Random(1557)
        pts = []
        taken = np.zeros((H // 2 + 1, W // 2 + 1), bool)

        def free(x, z, allowed):
            ix, iz = int(x), int(z)
            if not (2 <= ix < W - 2 and 2 <= iz < H - 2):
                return False
            if B[iz, ix] or G[iz, ix] not in allowed:
                return False
            return not taken[iz // 2, ix // 2]

        def add(kind, x, z, **kw):
            ix, iz = int(x), int(z)
            taken[iz // 2, ix // 2] = True
            rec = {"k": P_KINDS.index(kind), "x": ix + self.x0, "z": iz + self.z0}
            rec.update(kw)
            pts.append(rec)

        green = (GRASS, FOREST, FLOWERBED)
        # mapped trees and tree rows
        for nid, (la, lo, t) in self.nodes.items():
            if t.get("natural") == "tree":
                x, z = self.xy[nid][0] - self.x0, self.xy[nid][1] - self.z0
                if free(x, z, green + (SIDEWALK, PLAZA, PATH)):
                    add("conifer" if t.get("leaf_type") == "needleleaved" else "tree", x, z)
        for wid, (nds, t) in self.ways.items():
            if t.get("natural") == "tree_row":
                p = self.pts(nds)
                for (ax, az), (bx, bz) in zip(p, p[1:]):
                    L = math.hypot(bx - ax, bz - az)
                    k = 0.0
                    while k < L:
                        x, z = ax + (bx - ax) * k / max(L, 1e-6), az + (bz - az) * k / max(L, 1e-6)
                        if free(x, z, green + (SIDEWALK,)):
                            add("tree", x, z)
                        k += 7
        # parks and woods: jittered grid
        for outer, inner, t, _ in self.areas(lambda t: t.get("leisure") in ("park", "garden") or t.get("natural") in ("wood", "scrub") or t.get("landuse") in ("forest", "grass", "village_green")):
            dense = t.get("natural") in ("wood", "scrub") or t.get("landuse") == "forest"
            step = 5 if dense else (9 if t.get("leisure") in ("park", "garden") else 14)
            xs = [q[0] for r in outer for q in r]
            zs = [q[1] for r in outer for q in r]
            mask = Image.new("1", (int(max(xs) - min(xs)) + 2, int(max(zs) - min(zs)) + 2), 0)
            md = ImageDraw.Draw(mask)
            ox, oz = min(xs), min(zs)
            for r in outer:
                md.polygon([(q[0] - ox, q[1] - oz) for q in r], fill=1)
            for r in inner:
                md.polygon([(q[0] - ox, q[1] - oz) for q in r], fill=0)
            ma = np.asarray(mask)
            for gz in np.arange(0, ma.shape[0], step):
                for gx in np.arange(0, ma.shape[1], step):
                    x = gx + rnd.uniform(0, step)
                    z = gz + rnd.uniform(0, step)
                    if int(z) >= ma.shape[0] or int(x) >= ma.shape[1] or not ma[int(z), int(x)]:
                        continue
                    wx, wz = x + ox, z + oz
                    ok = free(wx, wz, green) and all(G[int(wz) + dz, int(wx) + dx] in green + (PATH,) and not B[int(wz) + dz, int(wx) + dx]
                                                     for dz in (-2, 0, 2) for dx in (-2, 0, 2))
                    if ok:
                        kind = rnd.choices(["tree", "birch", "conifer"], [6, 3, 2 if dense else 1])[0]
                        add(kind, wx, wz)
        # street trees, lamps
        for r in self.road_info:
            if r["hw"] == "service":
                continue
            p, w, sw = r["p"], r["w"], r["sw"]
            s_total = 0.0
            for (ax, az), (bx, bz) in zip(p, p[1:]):
                L = math.hypot(bx - ax, bz - az)
                if L < 0.01:
                    continue
                ux, uz = (bx - ax) / L, (bz - az) / L
                nx, nz = -uz, ux
                t = 0.0
                while t < L:
                    s = s_total + t
                    cx, cz = ax + ux * t, az + uz * t
                    for side in (-1, 1):
                        if (s + (0 if side > 0 else 14)) % 28 < 1.0:
                            lx, lz = cx + nx * side * (w / 2 + 0.8), cz + nz * side * (w / 2 + 0.8)
                            if free(lx, lz, (SIDEWALK, PLAZA)):
                                # the arm points towards the carriageway
                                add("lamp", lx, lz, d=[round(-nx * side, 2), round(-nz * side, 2)])
                        if (s + (4 if side > 0 else 9)) % 9 < 1.0:
                            tx, tz = cx + nx * side * (w / 2 + sw + 2.0), cz + nz * side * (w / 2 + sw + 2.0)
                            if free(tx, tz, (GRASS,)) and G[int(tz), int(tx)] == GRASS:
                                add("tree", tx, tz)
                    t += 1.0
                s_total += L
        # street furniture from the map
        for nid, (la, lo, t) in self.nodes.items():
            x, z = self.xy[nid][0] - self.x0, self.xy[nid][1] - self.z0
            if not (2 <= x < W - 2 and 2 <= z < H - 2):
                continue
            name = t.get("name", "")
            if t.get("amenity") == "bench" and free(x, z, (SIDEWALK, PLAZA, PATH, GRASS)):
                add("bench", x, z)
            elif t.get("highway") == "street_lamp" and free(x, z, (SIDEWALK, PLAZA, PATH, GRASS)):
                add("lamp", x, z, d=[0, 0])
            elif t.get("amenity") == "waste_basket" and free(x, z, (SIDEWALK, PLAZA, PATH)):
                add("waste", x, z)
            elif t.get("amenity") == "bicycle_parking" and free(x, z, (SIDEWALK, PLAZA)):
                add("bicycle", x, z)
            elif t.get("highway") == "bus_stop" or t.get("public_transport") == "platform":
                sx, sz, d = self.snap_to_sidewalk(G, B, x, z)
                if sx is not None:
                    add("bus_stop", sx, sz, n=name, d=d)
            elif t.get("highway") == "traffic_signals":
                for (qx, qz) in self.corners(G, B, x, z):
                    if not taken[int(qz) // 2, int(qx) // 2]:
                        add("signals", qx, qz, d=[round((x - qx) / max(1e-6, math.hypot(x - qx, z - qz)), 2), round((z - qz) / max(1e-6, math.hypot(x - qx, z - qz)), 2)])
            elif t.get("amenity") == "fountain" and free(x, z, (PLAZA, GRASS, SIDEWALK, PATH, FLOWERBED)):
                add("fountain", x, z, n=name)
            elif t.get("historic") in ("memorial", "monument") or t.get("tourism") == "artwork":
                kind = t.get("memorial") or t.get("artwork_type") or ""
                if kind in ("plaque", "graffiti", "mural", "blue_plaque", "stolperstein"):
                    continue
                k = "bust" if kind == "bust" else "stele" if kind in ("stele", "obelisk", "stone") else "statue" if kind in ("statue", "sculpture") else "memorial"
                if t.get("tourism") == "artwork" and kind not in ("statue", "bust"):
                    k = "artwork"
                if not B[int(z), int(x)]:
                    add(k, x, z, n=name)
            elif (t.get("shop") or t.get("amenity") in ("cafe", "fast_food", "pharmacy", "atm", "ice_cream", "kiosk")) and not B[int(z), int(x)]:
                if free(x, z, (SIDEWALK, PLAZA, GRASS, PATH, PARKING)) and t.get("shop") != "outpost":
                    add("kiosk", x, z, n=name, s=t.get("shop") or t.get("amenity"))
        # street name plates at junctions of two named streets
        seen = set()
        for r in self.road_info:
            for n in r["nds"]:
                if n in seen or not r["name"]:
                    continue
                names = {q["name"] for q in self.road_info if n in q["nds"] and q["name"]}
                if len(names) >= 2:
                    seen.add(n)
                    x, z = self.xy[n][0] - self.x0, self.xy[n][1] - self.z0
                    corners = self.corners(G, B, x, z)
                    if corners:
                        qx, qz = corners[0]
                        nm = sorted(names)
                        add("street_sign", qx, qz, n=nm[0], m=nm[1])
        return pts

    def snap_to_sidewalk(self, G, B, x, z):
        best = None
        for dz in range(-12, 13):
            for dx in range(-12, 13):
                ix, iz = int(x) + dx, int(z) + dz
                if 0 <= ix < G.shape[1] and 0 <= iz < G.shape[0] and G[iz, ix] == SIDEWALK and not B[iz, ix]:
                    dist = dx * dx + dz * dz
                    if best is None or dist < best[0]:
                        best = (dist, ix, iz)
        if best is None:
            return None, None, None
        _, ix, iz = best
        # face the nearest carriageway
        for rad in range(1, 8):
            for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                jx, jz = ix + d[0] * rad, iz + d[1] * rad
                if 0 <= jx < G.shape[1] and 0 <= jz < G.shape[0] and G[jz, jx] == ROAD:
                    return ix + 0.5, iz + 0.5, list(d)
        return ix + 0.5, iz + 0.5, [0, 0]

    def corners(self, G, B, x, z):
        out = []
        for qx, qz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            for rad in range(4, 16):
                ix, iz = int(x + qx * rad * 0.7), int(z + qz * rad * 0.7)
                if 0 <= ix < G.shape[1] and 0 <= iz < G.shape[0] and G[iz, ix] == SIDEWALK and not B[iz, ix]:
                    out.append((ix + 0.5, iz + 0.5))
                    break
        return out

    # ------------------------------------------------------------------ buildings
    def building_records(self, B, Hh):
        H, W = B.shape
        # points inside or on the outline of each building
        inside = {}
        for nid, (la, lo, t) in self.nodes.items():
            if not t:
                continue
            x, z = self.xy[nid][0] - self.x0, self.xy[nid][1] - self.z0
            ix, iz = int(x), int(z)
            if not (1 <= ix < W - 1 and 1 <= iz < H - 1):
                continue
            idx = B[iz, ix]
            if not idx:
                # entrances lie on the outline, often just outside the raster: look at the neighbours
                for dz in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        if B[iz + dz, ix + dx]:
                            idx = B[iz + dz, ix + dx]
                if not idx:
                    continue
            inside.setdefault(int(idx), []).append((nid, ix + self.x0, iz + self.z0, t))
        recs = []
        for i, b in enumerate(self.buildings, start=1):
            t = b["t"]
            zz, xx = np.nonzero(B == i) if False else (None, None)
            rec = {"i": i, "osm": b["osm"]}
            xs = [q[0] for r in b["outer"] for q in r]
            zs = [q[1] for r in b["outer"] for q in r]
            rec["bb"] = [int(math.floor(min(xs))) + self.x0 - 1, int(math.floor(min(zs))) + self.z0 - 1,
                         int(math.ceil(max(xs))) + self.x0 + 1, int(math.ceil(max(zs))) + self.z0 + 1]
            rec["y"] = int(self.base[i])
            for key, short in (("building", "type"), ("building:part", "part"), ("name", "name"), ("addr:street", "street"),
                               ("addr:housenumber", "num"), ("roof:shape", "roof"), ("building:material", "mat"),
                               ("building:colour", "col"), ("roof:colour", "rcol"), ("amenity", "amenity"), ("shop", "shop"),
                               ("office", "office"), ("tourism", "tourism"), ("religion", "religion"), ("historic", "historic")):
                if key in t:
                    rec[short] = t[key]
            lv = None
            for key in ("building:levels", "levels"):
                try:
                    lv = int(float(t.get(key, "").replace(",", ".")))
                    break
                except ValueError:
                    pass
            if lv is None and "height" in t:
                try:
                    lv = max(1, int(round(float(t["height"].split()[0]) / 3.2)))
                except ValueError:
                    pass
            if lv is not None:
                rec["lv"] = max(1, min(40, lv))
            try:
                rec["minlv"] = int(t.get("building:min_level", "0"))
            except ValueError:
                pass
            try:
                rec["rlv"] = int(t.get("roof:levels", "0"))
            except ValueError:
                pass
            area = 0.0
            for r in b["outer"]:
                area += abs(sum(r[k][0] * r[k - 1][1] - r[k - 1][0] * r[k][1] for k in range(len(r)))) / 2
            rec["area"] = int(area)
            ent, shops, plaques, pois = [], [], [], []
            for nid, x, z, nt in inside.get(i, []):
                if "entrance" in nt or nt.get("door"):
                    ent.append([x, z, nt.get("entrance", "yes"), nt.get("ref", nt.get("addr:flats", ""))])
                elif nt.get("memorial") == "plaque" or nt.get("historic") == "memorial" and nt.get("memorial") in ("plaque", "blue_plaque"):
                    plaques.append([x, z, nt.get("name", "")])
                elif nt.get("shop") or nt.get("amenity") in ("cafe", "restaurant", "fast_food", "pharmacy", "bank", "bar", "pub", "atm",
                                                              "dentist", "doctors", "clinic", "post_office", "ice_cream", "library", "cinema"):
                    shops.append([x, z, nt.get("shop") or nt.get("amenity"), nt.get("name", "")])
                elif nt.get("name") and (nt.get("amenity") or nt.get("office") or nt.get("tourism") or nt.get("leisure")):
                    pois.append([x, z, nt.get("amenity") or nt.get("office") or nt.get("tourism") or nt.get("leisure"), nt["name"], nid])
            if ent:
                rec["ent"] = ent
            if shops:
                rec["shops"] = shops
            if plaques:
                rec["plq"] = plaques
            if pois:
                rec["poi"] = pois
            recs.append(rec)
        return recs

    def places(self, brecs):
        by_osm = {r["osm"]: r for r in brecs}

        def containing(node_id):
            for r in brecs:
                for p in r.get("poi", []) + r.get("shops", []):
                    if len(p) > 4 and p[4] == node_id:
                        return r["i"]
            la, lo, _ = self.nodes[node_id]
            x, z = proj(la, lo)
            ix, iz = int(x - self.x0), int(z - self.z0)
            return int(self.B[iz, ix]) if 0 <= iz < self.B.shape[0] and 0 <= ix < self.B.shape[1] else 0

        def pos(node_id):
            la, lo, _ = self.nodes[node_id]
            x, z = proj(la, lo)
            return [int(round(x)), int(round(z))]

        roles = {}
        roles["bsmu_main"] = by_osm[95809664]["i"]
        roles["bsmu_anatomy"] = containing(9007181319)
        roles["bsmu_canteen"] = containing(9007181217) or containing(518699435)
        roles["dorm"] = containing(9007026017)
        roles["opera"] = by_osm[95399690]["i"]
        roles["gostiny"] = by_osm[60204196]["i"]
        roles["library"] = by_osm[95783308]["i"]
        for k, v in roles.items():
            r = brecs[v - 1] if v else None
            print("role", k, v, r and (r.get("street"), r.get("num"), r.get("name"), r.get("lv"), r.get("area")))
        spots = {"salavat": pos(656228306), "friendship": pos(11148564609), "lenin_park": pos(1368761682),
                 "gostiny_sq": pos(2004178889), "art_kvadrat": pos(7796147287), "pushkin": pos(3702818100)}
        return {"roles": roles, "spots": spots}

    # ------------------------------------------------------------------ preview
    def preview(self, out_dir, G, M, B, Hh, points):
        pal = {VOID: (90, 140, 70), GRASS: (106, 160, 80), ROAD: (70, 70, 74), SIDEWALK: (176, 170, 160), PLAZA: (200, 190, 170),
               PATH: (190, 170, 130), PARKING: (100, 100, 104), WATER: (60, 110, 200), SAND: (220, 210, 150), PITCH: (70, 150, 70),
               PLAYGROUND: (200, 120, 90), FLOWERBED: (200, 90, 140), FOREST: (60, 110, 50), DIRT: (130, 100, 70),
               YARD: (120, 120, 120), STEPS: (160, 160, 160), TRACK: (170, 80, 60), GARAGES: (140, 140, 140), FOUNTAIN: (80, 160, 230)}
        rgb = np.zeros(G.shape + (3,), np.uint8)
        for k, c in pal.items():
            rgb[G == k] = c
        shade = ((Hh.astype(np.float32) - WATER_Y) / 90.0).clip(0, 1)[..., None]
        rgb = (rgb * (0.75 + 0.25 * shade)).astype(np.uint8)
        rgb[M == M_LINE] = (240, 240, 240)
        rgb[M == M_ZEBRA] = (255, 255, 255)
        rgb[M == M_RAIL] = (40, 30, 30)
        rgb[M == M_CURB] = (210, 210, 210)
        rgb[B > 0] = (190, 120, 100)
        im = Image.fromarray(rgb)
        d = ImageDraw.Draw(im)
        for p in points:
            x, z = p["x"] - self.x0, p["z"] - self.z0
            if P_KINDS[p["k"]] in ("tree", "birch", "conifer"):
                d.point((x, z), fill=(20, 80, 20))
            elif P_KINDS[p["k"]] == "lamp":
                d.point((x, z), fill=(255, 230, 0))
        im.save(os.path.join(self.work, "preview_full.png"))
        im.crop((self.w // 2 - 900, self.h // 2 - 900, self.w // 2 + 900, self.h // 2 + 900)).save(os.path.join(self.work, "preview_center.png"))


def main():
    work = sys.argv[1] if len(sys.argv) > 1 else "osm_work"
    here = os.path.dirname(os.path.abspath(__file__))
    out_dir = sys.argv[2] if len(sys.argv) > 2 else os.path.join(here, "..", "src", "main", "resources", "ufamod_city")
    City(work).build(out_dir)


if __name__ == "__main__":
    main()
