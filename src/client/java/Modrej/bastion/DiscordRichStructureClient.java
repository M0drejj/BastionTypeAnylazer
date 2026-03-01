package Modrej.bastion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.structure.StructureKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

public class DiscordRichStructureClient implements ClientModInitializer {
    private static final Logger log = LoggerFactory.getLogger(DiscordRichStructureClient.class);
    // init variables
    private boolean wasInside = false;

    private enum BastionSection {
        RAMPART,
        TREASURE,
        HOUSING,
        STABLES
    }
    private BastionSection lastSection = null;
    private String lastBastionType = null;

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

    private Set<BastionSection> getBastionSections(StructureStart structureStart) {
        Set<BastionSection> sections = EnumSet.noneOf(BastionSection.class);

        for (var piece : structureStart.getChildren()) {

            if (!(piece instanceof PoolStructurePiece poolPiece)) continue;

            String id = poolPiece.getPoolElement().toString();

            if (id.contains("rampart")) sections.add(BastionSection.RAMPART);
            if (id.contains("treasure")) sections.add(BastionSection.TREASURE);
            if (id.contains("units")) sections.add(BastionSection.HOUSING);
            if (id.contains("stable")) sections.add(BastionSection.STABLES);
        }

        return sections;
    }

    private BastionSection getCurrentSection(StructureStart structureStart, BlockPos pos) {

        for (var piece : structureStart.getChildren()) {

            if (!(piece instanceof PoolStructurePiece poolPiece)) continue;

            if (!piece.getBoundingBox().contains(pos)) continue;

            String id = poolPiece.getPoolElement().toString();

            if (id.contains("rampart")) return BastionSection.RAMPART;
            if (id.contains("treasure")) return BastionSection.TREASURE;
            if (id.contains("units")) return BastionSection.HOUSING;
            if (id.contains("stable")) return BastionSection.STABLES;
        }

        return null;
    }
private String formatWSectionName(BastionSection section)
{
    String lower = section.name().toLowerCase();
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
}
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (FabricLoader.getInstance().isModLoaded("craftpresence")) {
                // sem přijde registrace variable
            }

                    if (client.world == null || client.player == null) return;
                    if (client.getServer() == null) return;

                    if (client.player.age % 20 != 0) return;

                    MinecraftServer server = client.getServer();
                    ServerWorld serverWorld = server.getWorld(client.world.getRegistryKey());
                    if (serverWorld == null) return;
                    BlockPos pos = client.player.getBlockPos();

                    if (!serverWorld.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;

                    StructureStart structureStart = FindBastion(serverWorld, pos);

                    if (structureStart == null) {
                        if(wasInside){
                            log.info("Player Left The Bastion!");
                        }
                        lastSection = null;
                        lastBastionType = null;
                        wasInside = false;
                        return;
                    }
                        Set<BastionSection> sections = getBastionSections(structureStart);
                        BastionSection currentSection = getCurrentSection(structureStart, pos);

                        String bastionType = null;

                        if (sections.contains(BastionSection.TREASURE)) {
                            bastionType = "Treasure Bastion";
                        } else if (sections.contains(BastionSection.HOUSING)) {
                            bastionType = "Housing Bastion";
                        } else if (sections.contains(BastionSection.STABLES)) {
                            bastionType = "Stables Bastion";
                        }

                        if (bastionType == null)
                            return;

                        if(!bastionType.equals(lastBastionType)) {
                            log.info("You entered: " + bastionType);
                            lastBastionType = bastionType;
                        }
                        if(currentSection != lastSection) {
                            if(currentSection != null)
                                log.info("You are in a " + formatWSectionName(currentSection));

                            lastSection = currentSection;
                        }
            wasInside = true;
                }


        );
    }
}


// This entrypoint is suitable for setting up client-specific logic, such as rendering.