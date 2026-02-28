package Modrej.bastion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.structure.StructureKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordRichStructureClient implements ClientModInitializer {
    private static final Logger log = LoggerFactory.getLogger(DiscordRichStructureClient.class);
    // init variables
    private int tickCounter = 0;
    private boolean wasInside = false;

    // init methods
    private StructureStart FindBastion(ServerWorld world, BlockPos pos) {
        var accessor = world.getStructureAccessor();
        var registryManager = world.getRegistryManager();
        var structureRegistry = registryManager.getOrThrow(RegistryKeys.STRUCTURE);
        var bastion = structureRegistry.get(StructureKeys.BASTION_REMNANT);

        if (bastion == null) return null;

        var structureStart = accessor.getStructureContaining(pos, bastion);

        if (structureStart == null || !structureStart.hasChildren()) return null;

        return structureStart;
    }

    private String getBastionType(StructureStart structureStart) {
        if (structureStart.getChildren().isEmpty()) return null;
        var firstPiece = structureStart.getChildren().get(0);

        if (firstPiece instanceof PoolStructurePiece poolPiece) {
            String id = poolPiece.getPoolElement().toString();

            if (id.contains("bastion/")) {
                return id.split("bastion/")[1].split("/")[0];
            }
        }
        return null;
    }

    private String formatName(String rawType) {
        String[] parts = rawType.split("_");
        StringBuilder formatted = new StringBuilder();

        for (String part : parts) {
            formatted.append(part.substring(0, 1).toUpperCase());
            formatted.append(part.substring((1)));
            formatted.append(" ");
        }
        return formatted.toString().trim();
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

                    if (client.world == null || client.player == null) return;
                    if (client.getServer() == null) return;

                    if (client.player.age % 20 != 0) return;

                    MinecraftServer server = client.getServer();
                    ServerWorld serverWorld = server.getWorld(client.world.getRegistryKey());
                    if (serverWorld == null) return;
                    BlockPos pos = client.player.getBlockPos();

                    if (!serverWorld.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;

                    StructureStart structureStart = FindBastion(serverWorld, pos);
                    boolean isInside = structureStart != null;
                    if (isInside && !wasInside) {
                        String rawType = getBastionType(structureStart);

                        if (rawType != null) {
                            String formatted = formatName(rawType);
                            log.info("Entered " + formatted + " Bastion");
                        }
                    }

                    if (!isInside && wasInside) {
                        log.info("Player Left The Bastion!");
                    }

                    wasInside = isInside;
                }


        );
    }
}







// This entrypoint is suitable for setting up client-specific logic, such as rendering.