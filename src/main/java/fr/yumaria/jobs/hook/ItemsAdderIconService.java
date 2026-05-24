package fr.yumaria.jobs.hook;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.job.IconDefinition;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public final class ItemsAdderIconService {
    private final YumariaJobsPlugin plugin;

    public ItemsAdderIconService(YumariaJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createIcon(IconDefinition icon) {
        if (icon.itemsAdderId() != null
                && !icon.itemsAdderId().isBlank()
                && plugin.getConfig().getBoolean("hooks.itemsadder.enabled", true)
                && plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            ItemStack itemStack = tryItemsAdder(icon.itemsAdderId());
            if (itemStack != null) {
                return itemStack;
            }
        }
        return new ItemStack(icon.material());
    }

    private ItemStack tryItemsAdder(String id) {
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method getInstance = customStackClass.getMethod("getInstance", String.class);
            Object customStack = getInstance.invoke(null, id);
            if (customStack == null) {
                return null;
            }
            Method getItemStack = customStackClass.getMethod("getItemStack");
            Object itemStack = getItemStack.invoke(customStack);
            if (itemStack instanceof ItemStack stack) {
                return stack;
            }
        } catch (ReflectiveOperationException exception) {
            plugin.debug("ItemsAdder icon failed for " + id + ": " + exception.getMessage());
        }
        return null;
    }
}
