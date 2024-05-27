package com.dairymoose.doggychunks;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(DoggyChunks.MODID)
public class DoggyChunks
{
	public static final String MODID = "doggychunks";
	
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static final DoggyLoader loader = new DoggyLoader();
    private static int DELAY_SECONDS = 10;
    private static int DELAY_SERVER_TICKS = 20 * DELAY_SECONDS;

    public DoggyChunks() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        //ForgeChunkManager.setForcedChunkLoadingCallback(DoggyChunks.MODID, loader);
    }
    
    private void setup(final FMLCommonSetupEvent event)
    {
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
    }

    private void processIMC(final InterModProcessEvent event)
    {
    }
    
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        //executor.scheduleAtFixedRate(loader, DELAY_SECONDS, DELAY_SECONDS, TimeUnit.SECONDS);
    }
    
    public long tickCount = 0;
    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
    	if (event.phase == TickEvent.Phase.START) {
    		++tickCount;
    		
    		if (tickCount % DELAY_SERVER_TICKS == 0) {
    			loader.loadDoggyChunks();
    		}
    	}
    }
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
    	//executor.shutdown();
        DoggyLoader.cleanup();
    }

}
