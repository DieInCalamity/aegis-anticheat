package com.aegis.check;

import com.aegis.Aegis;
import com.aegis.player.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class FastBreakCheck extends CheckBase implements Listener {
    public FastBreakCheck() {
        super("fastbreak");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;

        Player p = e.getPlayer();
        if (Aegis.getInstance().getConfigManager().isExempt(p)) return;

        Block block = e.getBlock();
        Material blockType = block.getType();
        ItemStack tool = p.getInventory().getItemInMainHand();

        if (!isCorrectTool(tool, blockType)) return;

        double hardness = getBlockHardness(blockType);
        if (hardness <= 0.0) return;

        Enchantment eff = Enchantment.getByName("DIG_SPEED");
        int efficiencyLevel = 0;
        if (eff != null && tool != null) efficiencyLevel = tool.getEnchantmentLevel(eff);

        double toolMultiplier = getToolMultiplier(tool, blockType);
        double efficiencyMultiplier = 1.0 + efficiencyLevel * 0.2;

        long expectedBreakTime = (long) ((hardness / (toolMultiplier * efficiencyMultiplier)) * 1000.0);

        PlayerData data = Aegis.getInstance().getData(p);
        long now = System.currentTimeMillis();
        long interval = now - data.lastBreakTime;
        data.lastBreakTime = now;

        long minInterval = Aegis.getInstance().getConfigManager().getInt("checks.fastbreak.min_interval_ms", 50);
        long threshold = Math.max(minInterval, expectedBreakTime);

        if (interval > 0 && interval < threshold) {
            fail(p, String.format("FastBreak detected: interval=%dms threshold=%dms hardness=%.2f toolMult=%.2f effMult=%.2f effLevel=%d correctTool=%b",
                    interval, threshold, hardness, toolMultiplier, efficiencyMultiplier, efficiencyLevel, isCorrectTool(tool, blockType)));
        }
    }

    private double getBlockHardness(Material type) {
        switch (type) {
            case STONE, COBBLESTONE, STONE_BRICKS, ANDESITE, DIORITE, GRANITE:
                return 1.5;
            case IRON_ORE, GOLD_ORE, DIAMOND_ORE, EMERALD_ORE, LAPIS_ORE, REDSTONE_ORE:
                return 3.0;
            case OBSIDIAN:
                return 50.0;
            case NETHERRACK:
                return 0.4;
            case SAND, GRAVEL:
                return 0.5;
            case DIRT, COARSE_DIRT, PODZOL:
                return 0.5;
            case GRASS_BLOCK:
                return 0.6;
            case OAK_PLANKS, BIRCH_PLANKS, SPRUCE_PLANKS, JUNGLE_PLANKS, ACACIA_PLANKS, DARK_OAK_PLANKS:
                return 2.0;
            case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG:
                return 2.0;
            case TNT:
                return 0.0;
            case OAK_LEAVES, BIRCH_LEAVES, SPRUCE_LEAVES, JUNGLE_LEAVES, ACACIA_LEAVES, DARK_OAK_LEAVES:
                return 0.2;
            default:
                return 0.0;
        }
    }

    private double getToolMultiplier(ItemStack tool, Material blockType) {
        if (tool == null) return 1.0;
        String name = tool.getType().name();
        if (name.endsWith("PICKAXE")) return 8.0;
        if (name.endsWith("SHOVEL")) return 6.0;
        if (name.endsWith("AXE")) return 6.0;
        if (name.endsWith("SWORD")) return 1.0;
        return 1.0;
    }

    private boolean isCorrectTool(ItemStack tool, Material block) {
        if (tool == null) {
            switch (block) {
                case SAND, GRAVEL, DIRT, GRASS_BLOCK:
                    return true;
                default:
                    return false;
            }
        }
        String t = tool.getType().name();
        switch (block) {
            case STONE, COBBLESTONE, IRON_ORE, GOLD_ORE, DIAMOND_ORE, EMERALD_ORE, LAPIS_ORE, REDSTONE_ORE, OBSIDIAN:
                return t.endsWith("PICKAXE");
            case SAND, GRAVEL, DIRT, COARSE_DIRT, PODZOL, GRASS_BLOCK:
                return t.endsWith("SHOVEL") || t.endsWith("HOE");
            case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG,
                 OAK_PLANKS, BIRCH_PLANKS, SPRUCE_PLANKS, JUNGLE_PLANKS, ACACIA_PLANKS, DARK_OAK_PLANKS:
                return t.endsWith("AXE");
            case OAK_LEAVES, BIRCH_LEAVES, SPRUCE_LEAVES, JUNGLE_LEAVES, ACACIA_LEAVES, DARK_OAK_LEAVES:
                return t.endsWith("SHEARS") || t.endsWith("AXE");
            default:
                return true;
        }
    }
}
