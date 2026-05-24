package fr.yumaria.jobs.util;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

import java.util.EnumSet;
import java.util.Set;

public final class MaterialMatcher {
    private static final Set<Material> MINER_BLOCKS = EnumSet.of(
            Material.STONE,
            Material.COBBLESTONE,
            Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE,
            Material.GRANITE,
            Material.DIORITE,
            Material.ANDESITE,
            Material.TUFF,
            Material.CALCITE,
            Material.DRIPSTONE_BLOCK,
            Material.NETHERRACK,
            Material.BLACKSTONE,
            Material.BASALT,
            Material.SMOOTH_BASALT,
            Material.END_STONE
    );

    private MaterialMatcher() {
    }

    public static boolean isLog(Material material) {
        return Tag.LOGS.isTagged(material);
    }

    public static boolean isMatureCrop(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    public static boolean isMinerBlock(Material material) {
        if (MINER_BLOCKS.contains(material)) {
            return true;
        }
        String name = material.name();
        return name.endsWith("_ORE") || name.endsWith("_RAW_ORE_BLOCK");
    }
}
