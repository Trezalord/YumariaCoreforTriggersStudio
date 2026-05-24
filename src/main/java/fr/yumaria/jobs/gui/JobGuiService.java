package fr.yumaria.jobs.gui;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.config.JobRegistry;
import fr.yumaria.jobs.config.LanguageService;
import fr.yumaria.jobs.data.PlayerData;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.hook.ItemsAdderIconService;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.job.JobPlaceholderService;
import fr.yumaria.jobs.job.PlayerJobService;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JobGuiService implements Listener {
    private final YumariaJobsPlugin plugin;
    private final JobRegistry jobRegistry;
    private final PlayerDataService playerDataService;
    private final PlayerJobService playerJobService;
    private final JobPlaceholderService placeholderService;
    private final ItemsAdderIconService iconService;
    private final LanguageService languageService;
    private FileConfiguration menuConfig;

    public JobGuiService(
            YumariaJobsPlugin plugin,
            JobRegistry jobRegistry,
            PlayerDataService playerDataService,
            PlayerJobService playerJobService,
            JobPlaceholderService placeholderService,
            ItemsAdderIconService iconService,
            LanguageService languageService
    ) {
        this.plugin = plugin;
        this.jobRegistry = jobRegistry;
        this.playerDataService = playerDataService;
        this.playerJobService = playerJobService;
        this.placeholderService = placeholderService;
        this.iconService = iconService;
        this.languageService = languageService;
    }

    public void reload() {
        File target = new File(plugin.getDataFolder(), "menus/jobs.yml");
        if (!target.isFile()) {
            plugin.saveResource("menus/jobs.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(target);
    }

    public void refreshOpenMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof MainJobsHolder) {
                openMain(player);
            } else if (holder instanceof DetailJobHolder detailJobHolder) {
                openDetail(player, detailJobHolder.jobId());
            }
        }
    }

    public void openMain(Player player) {
        MainJobsHolder holder = new MainJobsHolder();
        Inventory inventory = Bukkit.createInventory(holder, size("main-menu.size", 54), Text.color(menuConfig.getString("main-menu.title", "&8Métiers")));
        holder.inventory = inventory;
        fill(inventory, "main-menu.filler");

        ConfigurationSection slots = menuConfig.getConfigurationSection("main-menu.job-slots");
        int fallbackSlot = 10;
        for (JobDefinition job : jobRegistry.all()) {
            if (!job.enabled()) {
                continue;
            }
            int slot = slots == null ? fallbackSlot++ : slots.getInt(job.id(), fallbackSlot++);
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, jobItem(player, job, "main-menu.job-item"));
            }
        }
        player.openInventory(inventory);
    }

    public void openDetail(Player player, String jobId) {
        Optional<JobDefinition> optionalJob = jobRegistry.get(jobId);
        if (optionalJob.isEmpty()) {
            languageService.send(player, "commands.unknown-job");
            return;
        }
        JobDefinition job = optionalJob.get();
        DetailJobHolder holder = new DetailJobHolder(job.id());
        Map<String, String> placeholders = placeholders(player, job);
        Inventory inventory = Bukkit.createInventory(holder, size("detail-menu.size", 54), Text.color(Text.placeholders(menuConfig.getString("detail-menu.title", "&8%job_name%"), placeholders)));
        holder.inventory = inventory;
        fill(inventory, "detail-menu.filler");
        inventory.setItem(menuConfig.getInt("detail-menu.slots.info", 22), jobItem(player, job, "detail-menu.info-item"));
        inventory.setItem(menuConfig.getInt("detail-menu.slots.back", 49), configuredItem("detail-menu.back-item", Material.ARROW, Map.of()));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof MainJobsHolder) && !(holder instanceof DetailJobHolder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }

        if (holder instanceof MainJobsHolder) {
            handleMainClick(player, event.getSlot(), event.getClick());
            return;
        }

        if (holder instanceof DetailJobHolder) {
            int backSlot = menuConfig.getInt("detail-menu.slots.back", 49);
            if (event.getSlot() == backSlot) {
                openMain(player);
            }
        }
    }

    private void handleMainClick(Player player, int slot, ClickType clickType) {
        JobDefinition job = jobAtSlot(slot);
        if (job == null) {
            return;
        }
        PlayerData data = playerDataService.getOrLoad(player);
        PlayerJobData jobData = data.peekJob(job.id());

        if (clickType == ClickType.RIGHT) {
            openDetail(player, job.id());
            return;
        }
        if (clickType == ClickType.SHIFT_RIGHT) {
            if (!player.hasPermission("yumariajobs.leave")) {
                languageService.send(player, "commands.no-permission");
                return;
            }
            if (playerJobService.leave(player, job)) {
                languageService.send(player, "jobs.left", Map.of("%job_name%", job.displayName()));
            } else {
                languageService.send(player, "jobs.not-joined");
            }
            openMain(player);
            return;
        }
        if (clickType != ClickType.LEFT) {
            return;
        }
        if (jobData == null || !jobData.isJoined()) {
            if (!player.hasPermission("yumariajobs.join")) {
                languageService.send(player, "commands.no-permission");
                return;
            }
            if (playerJobService.join(player, job)) {
                languageService.send(player, "jobs.joined", Map.of("%job_name%", job.displayName()));
            } else {
                languageService.send(player, "jobs.already-joined");
            }
            openMain(player);
            return;
        }
        PlayerJobService.ToggleResult result = playerJobService.toggle(player, job);
        switch (result) {
            case ACTIVE -> languageService.send(player, "jobs.active", Map.of("%job_name%", job.displayName()));
            case INACTIVE -> languageService.send(player, "jobs.inactive", Map.of("%job_name%", job.displayName()));
            case ACTIVE_LIMIT -> languageService.send(player, "jobs.active-limit");
            case NOT_JOINED -> languageService.send(player, "jobs.not-joined");
        }
        openMain(player);
    }

    private JobDefinition jobAtSlot(int slot) {
        ConfigurationSection slots = menuConfig.getConfigurationSection("main-menu.job-slots");
        if (slots == null) {
            return null;
        }
        for (String jobId : slots.getKeys(false)) {
            if (slots.getInt(jobId) == slot) {
                return jobRegistry.get(jobId).orElse(null);
            }
        }
        return null;
    }

    private ItemStack jobItem(Player player, JobDefinition job, String path) {
        ItemStack itemStack = iconService.createIcon(job.icon());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }
        Map<String, String> placeholders = placeholders(player, job);
        meta.setDisplayName(Text.color(Text.placeholders(menuConfig.getString(path + ".name", "&d%job_name%"), placeholders)));
        List<String> lore = new ArrayList<>();
        for (String line : menuConfig.getStringList(path + ".lore")) {
            if (line.contains("%description%")) {
                if (job.description().isEmpty()) {
                    lore.add(Text.color(Text.placeholders(line.replace("%description%", ""), placeholders)));
                } else {
                    for (String descriptionLine : job.description()) {
                        lore.add(Text.color(Text.placeholders(line.replace("%description%", descriptionLine), placeholders)));
                    }
                }
                continue;
            }
            lore.add(Text.color(Text.placeholders(line, placeholders)));
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private Map<String, String> placeholders(Player player, JobDefinition job) {
        PlayerData data = playerDataService.getOrLoad(player);
        PlayerJobData jobData = data.peekJob(job.id());
        boolean joined = jobData != null && jobData.isJoined();
        if (jobData == null) {
            jobData = new PlayerJobData();
        }
        Map<String, String> placeholders = placeholderService.placeholders(player, job, jobData);
        String activeStatus;
        if (!joined) {
            activeStatus = menuConfig.getString("main-menu.job-item.not-joined-status", "&7Non rejoint");
        } else if (jobData.isActive()) {
            activeStatus = menuConfig.getString("main-menu.job-item.active-status", "&aActif");
        } else {
            activeStatus = menuConfig.getString("main-menu.job-item.inactive-status", "&cInactif");
        }
        placeholders.put("%active_status%", activeStatus);
        return placeholders;
    }

    private void fill(Inventory inventory, String path) {
        if (!menuConfig.getBoolean(path + ".enabled", true)) {
            return;
        }
        ItemStack filler = configuredItem(path, Material.BLACK_STAINED_GLASS_PANE, Map.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack configuredItem(String path, Material fallback, Map<String, String> placeholders) {
        Material material = Material.matchMaterial(menuConfig.getString(path + ".material", fallback.name()));
        if (material == null) {
            material = fallback;
        }
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(Text.placeholders(menuConfig.getString(path + ".name", " "), placeholders)));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private int size(String path, int fallback) {
        int size = menuConfig.getInt(path, fallback);
        size = Math.max(9, Math.min(54, size));
        return size - (size % 9);
    }

    private static final class MainJobsHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class DetailJobHolder implements InventoryHolder {
        private final String jobId;
        private Inventory inventory;

        private DetailJobHolder(String jobId) {
            this.jobId = jobId;
        }

        private String jobId() {
            return jobId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
