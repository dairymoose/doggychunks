package com.dairymoose.doggychunks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.world.ForgeChunkManager.LoadingValidationCallback;
import net.minecraftforge.common.world.ForgeChunkManager.TicketHelper;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public class DoggyLoader implements Runnable, LoadingValidationCallback {

	public static File file = null;
	public static final BlockPos origin = new BlockPos(0, 0, 0);
	
	public static List<ChunkInfo> loadedChunks = new ArrayList<ChunkInfo>();
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static String getFolderPath() {
		Path path = ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT);
		
		return path.toAbsolutePath().toString() + "\\data\\doggytalentsDogLocations.dat";
	}
	
	public static ResourceKey<Level> getKey(String dimension) {
		return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimension));
	}
	
	public int getQuoteCount(String text) {
		if (text == null)
			return 0;
		int sum = 0;
		for (int i=0; i<text.length(); ++i) {
			if (text.charAt(i) == '\'')
				++sum;
		}
		return sum;
	}
	
	@Override
	public void run() {
		loadDoggyChunks();
	}

	public void loadDoggyChunks() {
		if (file == null) {
			
			String folderPath = DoggyLoader.getFolderPath();
			file = new File(folderPath);
		}
		try {
			CompoundTag tag = NbtIo.readCompressed(file);
			List<ChunkInfo> chunksToUnload = new ArrayList<ChunkInfo>();
			chunksToUnload.addAll(loadedChunks);
			if (tag.contains("data")) {
				CompoundTag dataTag = tag.getCompound("data");
				if (dataTag != null && dataTag.contains("locationData")) {
					ListTag locationList = dataTag.getList("locationData", 10);
					for (int i=0; i<locationList.size(); ++i) {
						CompoundTag dogTag = locationList.getCompound(i);
						String nameComponent = dogTag.getString("name_text_component");
						int quotes = getQuoteCount(nameComponent);
						String dogName = "unknown";
						if (quotes == 4) {
							int quoteLastIdx = nameComponent.lastIndexOf('\'');
							int priorQuote = nameComponent.lastIndexOf('\'', quoteLastIdx);
							
							dogName = nameComponent.substring(priorQuote + 1, quoteLastIdx);
						}
						String dimension = dogTag.getString("dimension");
						double x = dogTag.getDouble("x");
						double y = dogTag.getDouble("y");
						double z = dogTag.getDouble("z");
						BlockPos dogPos = BlockPos.containing(x, y, z);
						//Minecraft.getInstance().player.displayClientMessage(new TextComponent("name=" + name + ", xyz=" + x +" " + y  + " "+ z), false);
						ChunkPos chunkCoord = new ChunkPos(dogPos);
						//Minecraft.getInstance().player.displayClientMessage(new TextComponent("chunk coords = " + chunkCoord), false);
						ChunkInfo chunkInfo = new ChunkInfo(dogName, chunkCoord, dimension);
						chunksToUnload.remove(chunkInfo);
						ServerLevel level = ServerLifecycleHooks.getCurrentServer().getLevel(getKey(dimension));
						if (!level.isLoaded(dogPos) && !DoggyLoader.loadedChunks.contains(chunkInfo)) {
							DoggyLoader.loadedChunks.add(chunkInfo);
							DoggyLoader.addChunk(ServerLifecycleHooks.getCurrentServer().getLevel(getKey(dimension)), chunkCoord);
						}
					}
				}
			}
			
			for (ChunkInfo chunkInfo : chunksToUnload) {
				DoggyLoader.removeChunk(ServerLifecycleHooks.getCurrentServer().getLevel(getKey(chunkInfo.dimension)), chunkInfo.pos);
				DoggyLoader.loadedChunks.remove(chunkInfo);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void cleanup() {
		for (ChunkInfo chunkInfo : DoggyLoader.loadedChunks) {
			DoggyLoader.removeChunk(ServerLifecycleHooks.getCurrentServer().getLevel(getKey(chunkInfo.dimension)), chunkInfo.pos);
		}
	}

	@Override
	public void validateTickets(ServerLevel world, TicketHelper ticketHelper) {
		Map<BlockPos, Pair<LongSet, LongSet>> tickets = ticketHelper.getBlockTickets();
		if (tickets != null && !tickets.isEmpty()) {
			LOGGER.info("[DoggyChunks] Validating tickets...");
			for (Map.Entry<BlockPos, Pair<LongSet, LongSet>> entry : tickets.entrySet()) {
				BlockPos key = entry.getKey();
				Pair<LongSet, LongSet> value = entry.getValue();
				
				LongSet chunks = value.getFirst();
				LongSet tickingChunks = value.getSecond();
				for (Long chunkLong : chunks) {
					ChunkPos chunkCoord = new ChunkPos(chunkLong);	
					
					DoggyLoader.removeChunk(ServerLifecycleHooks.getCurrentServer().overworld(), chunkCoord);
				}
				for (Long chunkLong : tickingChunks) {
					ChunkPos chunkCoord = new ChunkPos(chunkLong);	
					
					DoggyLoader.removeChunk(ServerLifecycleHooks.getCurrentServer().overworld(), chunkCoord);
				}
				
			}
		}
	}
	
	private static void addChunk(ServerLevel level, ChunkPos chunkCoord) {
		//if (ForgeChunkManager.forceChunk(level, DoggyChunks.MODID, origin, chunkCoord.x, chunkCoord.z, true, true)) {
		if (level.setChunkForced(chunkCoord.x, chunkCoord.z, true)) {
			LOGGER.info("[DoggyChunks] Load chunk success: " + chunkCoord);
		} else {
			LOGGER.info("[DoggyChunks] Load chunk failure: " + chunkCoord);
		}
	}
	
	private static void removeChunk(ServerLevel level, ChunkPos chunkCoord) {
		//if (ForgeChunkManager.forceChunk(level, DoggyChunks.MODID, origin, chunkCoord.x, chunkCoord.z, false, false)) {
		if (level.setChunkForced(chunkCoord.x, chunkCoord.z, false)) {
			LOGGER.info("[DoggyChunks] Unload chunk success: " + chunkCoord);
		} else {
			LOGGER.info("[DoggyChunks] Unload chunk failure: " + chunkCoord);
		}
	}
	
	public static class ChunkInfo {
		String dogName;
		ChunkPos pos;
		String dimension;
		
		ChunkInfo(String dogName, ChunkPos pos, String dimension) {
			this.pos = pos;
			this.dimension = dimension;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((dimension == null) ? 0 : dimension.hashCode());
			result = prime * result + ((pos == null) ? 0 : pos.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ChunkInfo other = (ChunkInfo) obj;
			if (dimension == null) {
				if (other.dimension != null)
					return false;
			} else if (!dimension.equals(other.dimension))
				return false;
			if (pos == null) {
				if (other.pos != null)
					return false;
			} else if (!pos.equals(other.pos))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ChunkInfo [dogName=" + dogName + " + pos=" + pos + ", dimension=" + dimension + "]";
		}
	}

}
