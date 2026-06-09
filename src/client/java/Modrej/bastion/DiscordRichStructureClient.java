package net.m0drejj.bastiontypeanalyzer.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
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

public class BastionTypeAnalyzerClient implements ClientModInitializer {
    private static final Logger log = LoggerFactory.getLogger(BastionTypeAnalyzerClient.class);

    // Stavy pro sledování pohybu hráče
    private boolean wasInsideBastion = false;
    private BastionSection lastSection = null;
    private String lastBastionType = null;

    private enum BastionSection {
        RAMPART, TREASURE, HOUSING, STABLES;

        // Přesunuli jsme formátování textu přímo do Enumu, což je čistější
        public String getFormattedName() {
            String lower = this.name().toLowerCase();
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
    }

    @Override
    public void onInitializeClient() {
        // Registrace pro CraftPresence (pokud je mod načtený)
        if (FabricLoader.getInstance().isModLoaded("craftpresence")) {
            // sem přijde případná registrace proměnných
        }

        // Samotný tick se teď stará jen o spuštění kontroly jednou za vteřinu
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (shouldExecuteCheck(client)) {
                analyzePlayerLocation(client);
            }
        });
    }

    /**
     * Vstupní filtr. Říká, zda má smysl v tomto ticku vůbec něco počítat.
     */
    private boolean shouldExecuteCheck(MinecraftClient client) {
        if (client.world == null || client.player == null) return false;
        if (client.getServer() == null) return false; // Pojistka pro multiplayer
        return client.player.age % 20 == 0;           // Kontrola každou vteřinu
    }

    /**
     * Hlavní mozek modu, který se spustí každou vteřinu.
     */
    private void analyzePlayerLocation(MinecraftClient client) {
        MinecraftServer server = client.getServer();
        ServerWorld serverWorld = server.getWorld(client.world.getRegistryKey());
        if (serverWorld == null) return;

        BlockPos pos = client.player.getBlockPos();
        if (!serverWorld.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;

        StructureStart bastion = findBastionStructure(serverWorld, pos);

        if (bastion == null) {
            handlePlayerOutsideBastion();
        } else {
            handlePlayerInsideBastion(bastion, pos);
        }
    }

    /**
     * Zpracuje situaci, kdy hráč prokazatelně nestojí v bastionu.
     */
    private void handlePlayerOutsideBastion() {
        if (wasInsideBastion) {
            log.info("Player Left The Bastion!");
        }
        // Kompletní reset vnitřní paměti
        lastSection = null;
        lastBastionType = null;
        wasInsideBastion = false;
    }

    /**
     * Zpracuje situaci, kdy hráč stojí uvnitř bastionu.
     */
    private void handlePlayerInsideBastion(StructureStart bastion, BlockPos pos) {
        wasInsideBastion = true;

        // 1. Zjistíme typ bastionu a vypíšeme ho, pokud se změnil
        String currentBastionType = determineBastionType(bastion);
        if (currentBastionType != null && !currentBastionType.equals(lastBastionType)) {
            log.info("You entered: " + currentBastionType);
            lastBastionType = currentBastionType;
        }

        // 2. Zjistíme aktuální místnost (sekci) a vypíšeme ji při změně
        BastionSection currentSection = getCurrentSection(bastion, pos);
        if (currentSection != lastSection) {
            if (currentSection != null) {
                log.info("You are in a " + currentSection.getFormattedName());
            }
            lastSection = currentSection;
        }
    }

    /**
     * Určí slovní název bastionu podle toho, jaké obsahuje části.
     */
    private String determineBastionType(StructureStart bastion) {
        Set<BastionSection> sections = getBastionSections(bastion);

        if (sections.contains(BastionSection.TREASURE)) return "Treasure Bastion";
        if (sections.contains(BastionSection.HOUSING))  return "Housing Bastion";
        if (sections.contains(BastionSection.STABLES))  return "Stables Bastion";
        
        return null; 
    }

    // --- PŮVODNÍ POMOCNÉ METODY PRO VYHLEDÁVÁNÍ V MINECRAFTU (jen lehce vyčištěné) ---

    private StructureStart findBastionStructure(ServerWorld world, BlockPos pos) {
        var structureRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        var bastionKey = structureRegistry.get(StructureKeys.BASTION_REMNANT);
        
        if (bastionKey == null) return null;
        
        StructureStart start = world.getStructureAccessor().getStructureContaining(pos, bastionKey);
        return (start != null && start.hasChildren()) ? start : null;
    }

    private Set<BastionSection> getBastionSections(StructureStart structureStart) {
        Set<BastionSection> sections = EnumSet.noneOf(BastionSection.class);
        for (var piece : structureStart.getChildren()) {
            if (piece instanceof PoolStructurePiece poolPiece) {
                String id = poolPiece.getPoolElement().toString();
                if (id.contains("rampart"))  sections.add(BastionSection.RAMPART);
                if (id.contains("treasure")) sections.add(BastionSection.TREASURE);
                if (id.contains("units"))    sections.add(BastionSection.HOUSING);
                if (id.contains("stable"))   sections.add(BastionSection.STABLES);
            }
        }
        return sections;
    }

    private BastionSection getCurrentSection(StructureStart structureStart, BlockPos pos) {
        for (var piece : structureStart.getChildren()) {
            if (piece instanceof PoolStructurePiece poolPiece && piece.getBoundingBox().contains(pos)) {
                String id = poolPiece.getPoolElement().toString();
                if (id.contains("rampart"))  return BastionSection.RAMPART;
                if (id.contains("treasure")) return BastionSection.TREASURE;
                if (id.contains("units"))    return BastionSection.HOUSING;
                if (id.contains("stable"))   return BastionSection.STABLES;
            }
        }
        return null;
    }
}
