#!/usr/bin/env python3
"""Downloads the raw data the city is built from: OpenStreetMap map tiles and terrain elevation tiles.

    python3 tools/fetch_osm.py <work dir>

Map data (c) OpenStreetMap contributors, ODbL. Elevation: Mapzen/AWS terrain tiles (SRTM and others).
The downloaded files are not committed; run tools/osm_to_city.py afterwards.
"""
import math
import os
import subprocess
import sys
import time

UA = "UfaCityMod/1.0 (minecraft hobby project)"
LAT_MIN, LAT_MAX, LON_MIN, LON_MAX = 54.7060, 54.7380, 55.9180, 55.9895


def curl(url, out):
    for _ in range(3):
        r = subprocess.run(["curl", "-sS", "-m", "120", "-A", UA, "-o", out, "-w", "%{http_code}", url], capture_output=True, text=True)
        if r.stdout == "200":
            return True
        time.sleep(5)
    return False


def main(work):
    tiles = os.path.join(work, "tiles")
    dem = os.path.join(work, "dem")
    os.makedirs(tiles, exist_ok=True)
    os.makedirs(dem, exist_ok=True)
    dl, dn = 0.004, 0.0065
    lat = LAT_MIN
    while lat < LAT_MAX - 1e-9:
        lon = LON_MIN
        while lon < LON_MAX - 1e-9:
            a, b = round(lat, 4), round(min(lat + dl, LAT_MAX), 4)
            c, d = round(lon, 4), round(min(lon + dn, LON_MAX), 4)
            out = os.path.join(tiles, f"t_{a}_{c}.xml")
            if not (os.path.exists(out) and os.path.getsize(out) > 500):
                ok = curl(f"https://api.openstreetmap.org/api/0.6/map?bbox={c},{a},{d},{b}", out)
                print(out, "ok" if ok else "FAILED")
                time.sleep(1)
            lon += dn
        lat += dl
    z = 15

    def tile(la, lo):
        n = 2 ** z
        return (lo + 180) / 360 * n, (1 - math.asinh(math.tan(math.radians(la))) / math.pi) / 2 * n

    x0, y0 = tile(LAT_MAX, LON_MIN)
    x1, y1 = tile(LAT_MIN, LON_MAX)
    for tx in range(int(x0), int(x1) + 1):
        for ty in range(int(y0), int(y1) + 1):
            out = os.path.join(dem, f"{tx}_{ty}.png")
            if not os.path.exists(out):
                curl(f"https://elevation-tiles-prod.s3.amazonaws.com/terrarium/{z}/{tx}/{ty}.png", out)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "osm_work")
