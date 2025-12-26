package com.example.dropmultiplier;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentState.Type;

public class PlacedBlockTracker extends PersistentState {
    private static final String STORAGE_KEY = "dropmultiplier_placed_blocks";
    private static final String CHUNKS_KEY = "chunks";
    private static final String CHUNK_POS_KEY = "chunk";
    private static final String POSITIONS_KEY = "positions";

    private final Long2ObjectMap<IntOpenHashSet> placedByChunk = new Long2ObjectOpenHashMap<>();

    public static final Type<PlacedBlockTracker> TYPE = new Type<>(
            PlacedBlockTracker::new,
            PlacedBlockTracker::fromNbt,
            DataFixTypes.LEVEL
    );

    public static PlacedBlockTracker get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
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
        if (set == null) {
            return;
        }
        if (set.remove(pack(world, pos))) {
            if (set.isEmpty()) {
                placedByChunk.remove(chunkKey);
            }
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList chunks = new NbtList();
        for (Long2ObjectMap.Entry<IntOpenHashSet> entry : placedByChunk.long2ObjectEntrySet()) {
            NbtCompound chunkTag = new NbtCompound();
            chunkTag.putLong(CHUNK_POS_KEY, entry.getLongKey());
            chunkTag.putIntArray(POSITIONS_KEY, entry.getValue().toIntArray());
            chunks.add(chunkTag);
        }
        nbt.put(CHUNKS_KEY, chunks);
        return nbt;
    }

    private static PlacedBlockTracker fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        PlacedBlockTracker tracker = new PlacedBlockTracker();
        NbtList chunks = nbt.getList(CHUNKS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < chunks.size(); i++) {
            NbtCompound chunkTag = chunks.getCompound(i).orElse(new NbtCompound());
            long chunkKey = chunkTag.getLong(CHUNK_POS_KEY);
            int[] positions = chunkTag.getIntArray(POSITIONS_KEY);
            if (positions.length == 0) {
                continue;
            }
            IntOpenHashSet set = new IntOpenHashSet(positions.length);
            for (int packed : positions) {
                set.add(packed);
            }
            tracker.placedByChunk.put(chunkKey, set);
        }
        return tracker;
    }

    private static int pack(ServerWorld world, BlockPos pos) {
        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;
        int localY = pos.getY() - world.getBottomY();
        return (localY << 8) | (localZ << 4) | localX;
    }
}