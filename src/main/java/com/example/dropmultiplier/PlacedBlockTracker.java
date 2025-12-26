package com.example.dropmultiplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlacedBlockTracker extends PersistentState {
    private static final String STORAGE_KEY = "dropmultiplier_placed_blocks";

    private final Long2ObjectMap<IntOpenHashSet> placedByChunk = new Long2ObjectOpenHashMap<>();

    private record ChunkEntry(long chunk, int[] positions) {
        private static final Codec<int[]> INT_ARRAY_CODEC = Codec.INT.listOf().xmap(
                list -> list.stream().mapToInt(Integer::intValue).toArray(),
                array -> Arrays.stream(array).boxed().toList()
        );

        private static final Codec<ChunkEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("chunk").forGetter(ChunkEntry::chunk),
                INT_ARRAY_CODEC.fieldOf("positions").forGetter(ChunkEntry::positions)
        ).apply(instance, ChunkEntry::new));
    }

    private static final Codec<PlacedBlockTracker> CODEC = ChunkEntry.CODEC.listOf().xmap(
            entries -> {
                PlacedBlockTracker tracker = new PlacedBlockTracker();
                for (ChunkEntry entry : entries) {
                    if (entry.positions == null || entry.positions.length == 0) {
                        continue;
                    }
                    IntOpenHashSet set = new IntOpenHashSet(entry.positions.length);
                    for (int packed : entry.positions) {
                        set.add(packed);
                    }
                    tracker.placedByChunk.put(entry.chunk, set);
                }
                return tracker;
            },
            tracker -> {
                List<ChunkEntry> entries = new ArrayList<>();
                for (Long2ObjectMap.Entry<IntOpenHashSet> entry : tracker.placedByChunk.long2ObjectEntrySet()) {
                    int[] positions = entry.getValue().toIntArray();
                    if (positions.length == 0) {
                        continue;
                    }
                    entries.add(new ChunkEntry(entry.getLongKey(), positions));
                }
                return entries;
            }
    );

    public static final PersistentStateType<PlacedBlockTracker> STATE_TYPE = new PersistentStateType<>(
            STORAGE_KEY,
            PlacedBlockTracker::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    public static PlacedBlockTracker get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(STATE_TYPE);
    }

    public void markPlaced(ServerWorld world, BlockPos pos) {
        long chunkKey = ChunkPos.toLong(pos);
        IntOpenHashSet set = placedByChunk.computeIfAbsent(chunkKey, key -> new IntOpenHashSet());
        set.add(pack(world, pos));
        markDirty();
    }

    public boolean isPlaced(ServerWorld world, BlockPos pos) {
        IntOpenHashSet set = placedByChunk.get(ChunkPos.toLong(pos));
        if (set == null) {
            return false;
        }
        return set.contains(pack(world, pos));
    }

    public void clearPlaced(ServerWorld world, BlockPos pos) {
        long chunkKey = ChunkPos.toLong(pos);
        IntOpenHashSet set = placedByChunk.get(chunkKey);
        if (set != null) {
            set.remove(pack(world, pos));
            if (set.isEmpty()) {
                placedByChunk.remove(chunkKey);
            }
            markDirty();
        }
    }

    private static int pack(ServerWorld world, BlockPos pos) {
        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;
        int localY = pos.getY() - world.getBottomY();
        return (localY << 8) | (localZ << 4) | localX;
    }
}
