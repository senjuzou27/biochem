package ru.ufamod.building;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ru.ufamod.UfaMod;
import ru.ufamod.city.BuildingInfo;
import ru.ufamod.city.CityData;
import ru.ufamod.world.Story;

import javax.annotation.Nullable;
import java.util.Optional;

/** Buildings are generated when the first of their chunks is needed and kept while their neighbourhood loads. */
public final class BuildingCache {
    private BuildingCache() {}

    private static final LoadingCache<Integer, Optional<Model>> CACHE = CacheBuilder.newBuilder()
            .maximumWeight(40_000_000L)
            .weigher((Integer k, Optional<Model> v) -> v.map(Model::volume).orElse(1))
            .build(new CacheLoader<>() {
                @Override
                public Optional<Model> load(Integer index) {
                    CityData city = CityData.get();
                    BuildingInfo b = city.buildingInfo(index);
                    try {
                        return Optional.ofNullable(Story.build(city, b));
                    } catch (Throwable t) {
                        UfaMod.LOGGER.error("Could not generate building {} ({})", index, b == null ? "?" : b.address(), t);
                        return Optional.empty();
                    }
                }
            });

    @Nullable
    public static Model get(BuildingInfo b) {
        return CACHE.getUnchecked(b.index).orElse(null);
    }
}
