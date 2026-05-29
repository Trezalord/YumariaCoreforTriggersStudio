package fr.yumaria.jobs.hook;

// Repere fichier YumariaJobs: integration optionnelle avec plugins externes (YumariaFishingHook).

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.config.JobRegistry;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.progress.JobProgressService;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Role YumariaJobs: Branche les integrations optionnelles sans dependance obligatoire.
public final class YumariaFishingHook implements Listener {
    private static final String SOURCE = "yumaria_fish_catch";
    private static final long DUPLICATE_GUARD_MS = 750L;

    private final YumariaJobsPlugin plugin;
    private final JobRegistry jobRegistry;
    private final PlayerDataService playerDataService;
    private final JobProgressService progressService;
    private final Map<String, Long> recentGrants = new ConcurrentHashMap<>();
    private final Map<UUID, PendingCatch> pendingCatches = new ConcurrentHashMap<>();
    private final Map<UUID, CustomCatchWatch> customCatchWatches = new ConcurrentHashMap<>();
    private final List<String> registeredCustomEvents = new ArrayList<>();
    private Plugin yumariaFishingPlugin;
    private boolean enabled;
    private boolean pluginPresent;
    private boolean fallbackRegistered;

    public YumariaFishingHook(
            YumariaJobsPlugin plugin,
            JobRegistry jobRegistry,
            PlayerDataService playerDataService,
            JobProgressService progressService
    ) {
        this.plugin = plugin;
        this.jobRegistry = jobRegistry;
        this.playerDataService = playerDataService;
        this.progressService = progressService;
    }

    // Annotation YumariaJobs: Recharge la configuration sans effacer les donnees joueur en memoire.
    public void reload() {
        shutdown();
        if (!plugin.getConfig().getBoolean("integrations.yumaria-fishing.enabled", true)) {
            enabled = false;
            pluginPresent = Bukkit.getPluginManager().isPluginEnabled("YumariaFishing");
            debug("YumariaFishing hook disabled by config. pluginPresent=" + pluginPresent);
            return;
        }

        Plugin yumariaFishing = Bukkit.getPluginManager().getPlugin("YumariaFishing");
        pluginPresent = yumariaFishing != null && yumariaFishing.isEnabled();
        if (!pluginPresent) {
            enabled = false;
            yumariaFishingPlugin = null;
            debug("YumariaFishing hook disabled: plugin is not enabled.");
            return;
        }

        yumariaFishingPlugin = yumariaFishing;
        enabled = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        fallbackRegistered = true;
        registerCustomCatchEvents(yumariaFishing);
        debug("YumariaFishing hook enabled. fallbackRegistered=" + fallbackRegistered
                + ", customEvents=" + registeredCustomEvents
                + ", jobId=" + jobId());
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void shutdown() {
        HandlerList.unregisterAll(this);
        registeredCustomEvents.clear();
        fallbackRegistered = false;
        enabled = false;
        yumariaFishingPlugin = null;
        recentGrants.clear();
        for (PendingCatch pendingCatch : pendingCatches.values()) {
            pendingCatch.cancel();
        }
        pendingCatches.clear();
        for (CustomCatchWatch watch : customCatchWatches.values()) {
            watch.cancel();
        }
        customCatchWatches.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPluginPresent() {
        return pluginPresent;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String status() {
        return "enabled=" + enabled
                + ", pluginPresent=" + pluginPresent
                + ", fallbackRegistered=" + fallbackRegistered
                + ", customEvents=" + (registeredCustomEvents.isEmpty() ? "-" : String.join(",", registeredCustomEvents))
                + ", activeWatches=" + customCatchWatches.size()
                + ", progressMode=" + progressMode();
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void onPlayerFish(PlayerFishEvent event) {
        if (!enabled) {
            return;
        }
        debug("PlayerFishEvent received: player=" + event.getPlayer().getName()
                + ", state=" + event.getState()
                + ", caught=" + (event.getCaught() == null ? "-" : event.getCaught().getType().name()));

        if (event.getState() == PlayerFishEvent.State.FISHING) {
            debug("PlayerFishEvent ignored: state=FISHING reason=rod_cast_no_confirmed_catch");
            return;
        }
        maybeStartCustomGameWatch(event.getPlayer(), "PlayerFishEvent:" + event.getState());
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            debug("PlayerFishEvent ignored: state=" + event.getState() + " reason=not_confirmed_caught_fish");
            return;
        }

        debug("[YumariaJobs] confirmed custom fish catch candidate: player=" + event.getPlayer().getName()
                + ", state=CAUGHT_FISH"
                + ", caughtEntityType=" + (event.getCaught() == null ? "-" : event.getCaught().getType().name()));
        if (event.getCaught() instanceof Item item) {
            debug("CAUGHT_FISH caught item stack extracted: type=" + item.getItemStack().getType().name()
                    + ", amount=" + item.getItemStack().getAmount());
            Optional<FishContext> context = contextFromItem(item.getItemStack());
            if (context.isPresent()) {
                debug("CAUGHT_FISH immediate item detection success: species=" + context.get().speciesId()
                        + ", rarity=" + context.get().rarity()
                        + ", quality=" + context.get().quality());
                grantProgress(event.getPlayer(), context.get(), "confirmed_catch:PlayerFishEvent caught item");
                return;
            }
            debug("CAUGHT_FISH item entity did not match yet; scheduling delayed confirmed catch scans.");
        } else {
            debug("CAUGHT_FISH had no item entity; scheduling delayed confirmed catch scans.");
        }

        startPendingCatch(event.getPlayer(), event.getCaught());
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled || event.getHand() != EquipmentSlot.HAND || !isFishingRod(event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK
                || event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            maybeStartCustomGameWatch(event.getPlayer(), "PlayerInteractEvent:" + event.getAction());
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if (!enabled || event.getAnimationType() != PlayerAnimationType.ARM_SWING || !isFishingRod(event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }
        maybeStartCustomGameWatch(event.getPlayer(), "PlayerAnimationEvent:ARM_SWING");
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    // Annotation YumariaJobs: Point d entree Bukkit appele par un evenement serveur.
    public void onQuit(PlayerQuitEvent event) {
        finishPendingCatch(event.getPlayer().getUniqueId(), "player quit");
        finishCustomCatchWatch(event.getPlayer().getUniqueId(), "player quit");
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    // Annotation YumariaJobs: Point d entree Bukkit appele par un evenement serveur.
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!enabled || !(event.getEntity() instanceof Player player)) {
            return;
        }
        debug("EntityPickupItemEvent received: player=" + player.getName()
                + ", item=" + event.getItem().getItemStack().getType().name());
        PendingCatch pendingCatch = pendingCatches.get(player.getUniqueId());
        if (pendingCatch == null) {
            debug("EntityPickupItemEvent ignored: no pending confirmed catch window for player=" + player.getName());
            return;
        }
        contextFromItem(event.getItem().getItemStack())
                .ifPresentOrElse(
                        context -> {
                            debug("EntityPickupItemEvent detection success during confirmed catch window: species=" + context.speciesId()
                                    + ", rarity=" + context.rarity()
                                    + ", quality=" + context.quality());
                            if (grantProgress(player, context, "confirmed_catch:EntityPickupItemEvent detected fish item")) {
                                finishPendingCatch(player.getUniqueId(), "pickup item grant");
                            }
                        },
                        () -> debug("EntityPickupItemEvent ignored: item did not match YumariaFishing detection rules")
                );
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public List<String> describeItem(ItemStack itemStack) {
        List<String> lines = new ArrayList<>();
        DetectionReport report = inspectItem(itemStack);
        lines.add("&dYumariaJobs &8» &fItem debug");
        if (itemStack == null || itemStack.getType().isAir()) {
            lines.add("&cNo item in main hand.");
            lines.add("&7YumariaFishing match: &cfalse");
            lines.add("&7Reason: &eempty item");
            return lines;
        }

        lines.add("&7Type: &e" + itemStack.getType().name());
        lines.add("&7Amount: &e" + itemStack.getAmount());
        if (!itemStack.hasItemMeta()) {
            lines.add("&7ItemMeta: &e-");
            lines.add("&7YumariaFishing match: &cfalse");
            lines.add("&7Reason: &eno item meta");
            return lines;
        }

        ItemMeta meta = itemStack.getItemMeta();
        lines.add("&7ItemMeta class: &e" + (meta == null ? "-" : meta.getClass().getName()));
        if (meta == null) {
            lines.add("&7YumariaFishing match: &cfalse");
            lines.add("&7Reason: &enull item meta");
            return lines;
        }

        lines.add("&7Display name: &e" + (meta.hasDisplayName() ? meta.getDisplayName() : "-"));
        lines.add("&7Custom model data: &e" + (meta.hasCustomModelData() ? meta.getCustomModelData() : "-"));
        lines.add("&7ItemsAdder id: &e" + (report.itemsAdderId().isBlank() ? "-" : report.itemsAdderId()));

        List<String> lore = meta.hasLore() && meta.getLore() != null ? meta.getLore() : List.of();
        lines.add("&7Lore lines: &e" + lore.size());
        for (int index = 0; index < lore.size(); index++) {
            lines.add("&8  [" + index + "] &f" + lore.get(index));
        }

        List<PdcValue> pdcValues = report.pdcValues();
        lines.add("&7PDC keys: &e" + pdcValues.size());
        if (pdcValues.isEmpty()) {
            lines.add("&8  -");
        }
        for (PdcValue value : pdcValues) {
            lines.add("&8  - &f" + value.key()
                    + " &7namespace=&e" + value.key().getNamespace()
                    + " &7key=&e" + value.key().getKey());
            lines.add("&8    &7STRING=&e" + value.stringValue()
                    + " &7INTEGER=&e" + value.integerValue()
                    + " &7DOUBLE=&e" + value.doubleValue()
                    + " &7FLOAT=&e" + value.floatValue()
                    + " &7LONG=&e" + value.longValue());
        }

        lines.add("&7Detection namespace match: &e" + report.namespaceMatch());
        lines.add("&7Detection key match: &e" + report.keyMatch());
        lines.add("&7Detection ItemsAdder match: &e" + report.itemsAdderMatch());
        lines.add("&7Name/lore fish markers: &e" + report.textMarkerMatch());
        lines.add(report.matched() ? "&7YumariaFishing match: &atrue" : "&7YumariaFishing match: &cfalse");
        for (String reason : report.reasons()) {
            lines.add("&7Reason: &e" + reason);
        }
        report.context().ifPresent(context -> {
            lines.add("&7Detected species: &e" + context.speciesId());
            lines.add("&7Detected rarity: &e" + context.rarity());
            lines.add("&7Detected quality: &e" + context.quality());
            lines.add("&7Detected category: &e" + context.category());
            lines.add("&7Detected sizeCm: &e" + context.sizeCm());
            lines.add("&7Detected weightKg: &e" + context.weightKg());
            lines.add("&7Detected baseValue: &e" + context.baseValue());
        });
        return lines;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private Optional<FishContext> contextFromItem(ItemStack itemStack) {
        return inspectItem(itemStack).context();
    }

    private boolean isFishingRod(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.FISHING_ROD;
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    private void startPendingCatch(Player player, Entity caughtEntity) {
        UUID uuid = player.getUniqueId();
        finishPendingCatch(uuid, "replaced by new confirmed catch");

        Map<String, Integer> beforeCounts = matchingInventoryCounts(player);
        PendingCatch pendingCatch = new PendingCatch(uuid, caughtEntity, beforeCounts);
        pendingCatches.put(uuid, pendingCatch);

        int delayTicks = Math.max(1, plugin.getConfig().getInt("integrations.yumaria-fishing.catch-detection-delay-ticks", 2));
        debug("Pending confirmed catch started: player=" + player.getName()
                + ", delayTicks=" + delayTicks
                + ", caughtEntity=" + (caughtEntity == null ? "-" : caughtEntity.getType().name())
                + ", beforeMatchingFish=" + beforeCounts);

        for (int tick = 1; tick <= delayTicks; tick++) {
            final int attemptTick = tick;
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> scanPendingCatch(player, attemptTick), attemptTick);
            pendingCatch.tasks().add(task);
        }
        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> finishPendingCatch(uuid, "confirmed catch window expired"), delayTicks + 20L);
        pendingCatch.tasks().add(expiryTask);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private void scanPendingCatch(Player player, int attemptTick) {
        PendingCatch pendingCatch = pendingCatches.get(player.getUniqueId());
        if (pendingCatch == null || !player.isOnline()) {
            return;
        }

        debug("Scanning pending confirmed catch: player=" + player.getName()
                + ", attemptTick=" + attemptTick
                + ", caughtEntity=" + (pendingCatch.caughtEntity() == null ? "-" : pendingCatch.caughtEntity().getType().name()));

        Optional<FishContext> entityContext = contextFromCaughtEntity(pendingCatch.caughtEntity());
        if (entityContext.isPresent()) {
            debug("Delayed caught entity detection success: player=" + player.getName()
                    + ", attemptTick=" + attemptTick
                    + ", species=" + entityContext.get().speciesId());
            if (grantProgress(player, entityContext.get(), "confirmed_catch:delayed caught entity scan tick " + attemptTick)) {
                finishPendingCatch(player.getUniqueId(), "delayed caught entity grant");
            }
            return;
        }

        Optional<FishContext> inventoryContext = newInventoryFishContext(player, pendingCatch.beforeCounts());
        if (inventoryContext.isPresent()) {
            debug("Delayed inventory detection success: player=" + player.getName()
                    + ", attemptTick=" + attemptTick
                    + ", species=" + inventoryContext.get().speciesId());
            if (grantProgress(player, inventoryContext.get(), "confirmed_catch:delayed inventory scan tick " + attemptTick)) {
                finishPendingCatch(player.getUniqueId(), "delayed inventory grant");
            }
            return;
        }

        debug("Delayed confirmed catch scan found no new YumariaFishing fish: player=" + player.getName()
                + ", attemptTick=" + attemptTick);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private Optional<FishContext> contextFromCaughtEntity(Entity caughtEntity) {
        if (!(caughtEntity instanceof Item item)) {
            return Optional.empty();
        }
        return contextFromItem(item.getItemStack());
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private Optional<FishContext> newInventoryFishContext(Player player, Map<String, Integer> beforeCounts) {
        Map<String, Integer> currentCounts = new HashMap<>();
        Map<String, FishContext> contexts = new HashMap<>();
        for (ItemStack itemStack : player.getInventory().getContents()) {
            Optional<FishContext> context = contextFromItem(itemStack);
            if (context.isEmpty()) {
                continue;
            }
            String fingerprint = context.get().fingerprint();
            currentCounts.merge(fingerprint, itemStack == null ? 0 : itemStack.getAmount(), Integer::sum);
            contexts.putIfAbsent(fingerprint, context.get());
        }

        for (Map.Entry<String, Integer> entry : currentCounts.entrySet()) {
            String fingerprint = entry.getKey();
            int before = beforeCounts.getOrDefault(fingerprint, 0);
            int current = entry.getValue();
            if (current > before) {
                debug("Inventory fish count increased: fingerprint=" + fingerprint
                        + ", beforeTotal=" + before
                        + ", currentTotal=" + current);
                return Optional.ofNullable(contexts.get(fingerprint));
            }
            debug("Inventory fish matched but was already present: fingerprint=" + fingerprint
                    + ", beforeTotal=" + before
                    + ", currentTotal=" + current);
        }
        return Optional.empty();
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private Map<String, Integer> matchingInventoryCounts(Player player) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack itemStack : player.getInventory().getContents()) {
            Optional<FishContext> context = contextFromItem(itemStack);
            if (context.isPresent()) {
                counts.merge(context.get().fingerprint(), itemStack == null ? 0 : itemStack.getAmount(), Integer::sum);
            }
        }
        return counts;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private void finishPendingCatch(UUID uuid, String reason) {
        PendingCatch pendingCatch = pendingCatches.remove(uuid);
        if (pendingCatch == null) {
            return;
        }
        pendingCatch.cancel();
        debug("Pending confirmed catch finished: uuid=" + uuid + ", reason=" + reason);
    }

    // Annotation YumariaJobs: Gere l affichage ou le cycle de vie d un feedback visuel.
    private void maybeStartCustomGameWatch(Player player, String source) {
        if (!plugin.getConfig().getBoolean("integrations.yumaria-fishing.catch-watch.enabled", true)) {
            return;
        }

        YumariaFishingState state = yumariaFishingState(player);
        if (!state.active() && !state.hookPending()) {
            debug("Custom catch watch not started: player=" + player.getName()
                    + ", source=" + source
                    + ", active=false, hookPending=false"
                    + (state.available() ? "" : ", runtime unavailable"));
            return;
        }

        UUID uuid = player.getUniqueId();
        CustomCatchWatch existing = customCatchWatches.get(uuid);
        if (existing != null) {
            currentGameSnapshot(player).ifPresent(existing::setLastSnapshot);
            return;
        }

        long scanIntervalTicks = Math.max(1L, plugin.getConfig().getLong("integrations.yumaria-fishing.catch-watch.scan-interval-ticks", 2L));
        long maxDurationTicks = Math.max(scanIntervalTicks, plugin.getConfig().getLong("integrations.yumaria-fishing.catch-watch.max-duration-ticks", 2400L));
        long graceTicks = Math.max(scanIntervalTicks, plugin.getConfig().getLong("integrations.yumaria-fishing.catch-watch.grace-after-inactive-ticks", 20L));
        CustomCatchWatch watch = new CustomCatchWatch(
                uuid,
                matchingInventoryCounts(player),
                source,
                scanIntervalTicks,
                maxDurationTicks,
                graceTicks
        );
        currentGameSnapshot(player).ifPresent(watch::setLastSnapshot);
        customCatchWatches.put(uuid, watch);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> scanCustomCatchWatch(player), scanIntervalTicks, scanIntervalTicks);
        watch.setTask(task);
        debug("Custom catch watch started: player=" + player.getName()
                + ", source=" + source
                + ", active=" + state.active()
                + ", hookPending=" + state.hookPending()
                + ", scanIntervalTicks=" + scanIntervalTicks
                + ", maxDurationTicks=" + maxDurationTicks
                + ", graceAfterInactiveTicks=" + graceTicks
                + ", beforeMatchingFish=" + watch.beforeCounts());
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private void scanCustomCatchWatch(Player player) {
        UUID uuid = player.getUniqueId();
        CustomCatchWatch watch = customCatchWatches.get(uuid);
        if (watch == null) {
            return;
        }
        if (!player.isOnline()) {
            finishCustomCatchWatch(uuid, "player offline");
            return;
        }

        watch.addElapsedTicks();
        YumariaFishingState state = yumariaFishingState(player);
        currentGameSnapshot(player).ifPresent(watch::setLastSnapshot);
        if (watch.elapsedTicks() >= watch.maxDurationTicks()) {
            finishCustomCatchWatch(uuid, "max duration elapsed");
            return;
        }

        Optional<FishContext> inventoryContext = newInventoryFishContext(player, watch.beforeCounts());
        if (inventoryContext.isPresent()) {
            FishContext context = inventoryContext.get().withGameSnapshot(watch.lastSnapshot());
            debug("[YumariaJobs] confirmed custom fish catch: player=" + player.getName()
                    + ", source=custom_game_watch"
                    + ", active=" + state.active()
                    + ", hookPending=" + state.hookPending()
                    + ", species=" + context.speciesId()
                    + ", rarity=" + context.rarity()
                    + ", quality=" + context.quality()
                    + ", castQuality=" + context.castQuality()
                    + ", catchPerformance=" + context.catchPerformance()
                    + ", perfectCatch=" + context.perfectCatch());
            if (grantProgress(player, context, "confirmed_catch:yumaria_fishing_game_inventory_watch")) {
                finishCustomCatchWatch(uuid, "inventory fish grant");
            }
            return;
        }

        if (state.active() || state.hookPending()) {
            watch.resetInactiveTicks();
            return;
        }

        watch.addInactiveTicks();
        if (watch.inactiveTicks() >= watch.graceAfterInactiveTicks()) {
            finishCustomCatchWatch(uuid, "inactive grace elapsed without fish item");
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private void finishCustomCatchWatch(UUID uuid, String reason) {
        CustomCatchWatch watch = customCatchWatches.remove(uuid);
        if (watch == null) {
            return;
        }
        watch.cancel();
        debug("Custom catch watch finished: uuid=" + uuid + ", reason=" + reason);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private DetectionReport inspectItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir() || !itemStack.hasItemMeta()) {
            return DetectionReport.empty(itemStack == null || itemStack.getType().isAir() ? "empty item" : "no item meta");
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return DetectionReport.empty("null item meta");
        }

        List<PdcValue> pdcValues = readPdcValues(meta.getPersistentDataContainer());
        List<String> reasons = new ArrayList<>();
        boolean namespaceMatch = false;
        boolean keyMatch = false;
        for (PdcValue value : pdcValues) {
            String namespace = Text.normalizeLookup(value.key().getNamespace());
            String key = Text.normalizeLookup(value.key().getKey());
            if (configuredValues("integrations.yumaria-fishing.detection.pdc-namespaces", List.of(
                    "yumariafishing", "yumaria_fishing", "yumaria", "yfish", "gukuan"
            )).stream().map(Text::normalizeLookup).anyMatch(namespace::contains)) {
                namespaceMatch = true;
                reasons.add("PDC namespace matched " + value.key().getNamespace());
            }
            if (configuredValues("integrations.yumaria-fishing.detection.pdc-key-contains", List.of(
                    "species", "fish", "rarity", "quality", "size", "weight", "base", "category"
            )).stream().map(Text::normalizeLookup).anyMatch(key::contains)) {
                keyMatch = true;
                reasons.add("PDC key matched " + value.key().getKey());
            }
        }

        String itemsAdderId = itemsAdderId(itemStack);
        boolean itemsAdderMatch = looksLikeYumariaFishItemsAdderId(itemsAdderId);
        if (itemsAdderMatch) {
            reasons.add("ItemsAdder id matched " + itemsAdderId);
        }

        boolean textMarkerMatch = hasTextFishMarker(meta);
        if (textMarkerMatch) {
            reasons.add("Display/lore contains fish-like rarity/quality marker (debug only)");
        }

        boolean matched = (namespaceMatch && keyMatch) || itemsAdderMatch;
        if (!matched) {
            if (!namespaceMatch) {
                reasons.add("No configured PDC namespace matched");
            }
            if (!keyMatch) {
                reasons.add("No configured PDC key fragment matched");
            }
            if (itemsAdderId.isBlank()) {
                reasons.add("No ItemsAdder id was readable");
            } else if (!itemsAdderMatch) {
                reasons.add("ItemsAdder id did not look like a Yumaria fish: " + itemsAdderId);
            }
        }

        Optional<FishContext> context = matched ? Optional.of(contextFromPdc(meta.getPersistentDataContainer(), itemsAdderId)) : Optional.empty();
        return new DetectionReport(matched, namespaceMatch, keyMatch, itemsAdderMatch, textMarkerMatch, itemsAdderId, pdcValues, reasons, context);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private FishContext contextFromPdc(PersistentDataContainer container, String itemsAdderId) {
        String speciesId = FishContext.readString(container, "speciesId", "species_id", "species-id", "species", "fish", "fish_id", "fishid");
        if (speciesId.isBlank() && !itemsAdderId.isBlank()) {
            speciesId = itemsAdderId;
        }
        return new FishContext(
                speciesId,
                FishContext.readString(container, "rarity", "fish_rarity"),
                FishContext.readString(container, "quality", "fish_quality"),
                FishContext.readString(container, "category", "fish_category"),
                FishContext.readDouble(container, "sizeCm", "size_cm", "size"),
                FishContext.readDouble(container, "weightKg", "weight_kg", "weight"),
                FishContext.readDouble(container, "baseValue", "base_value", "base", "value"),
                "",
                null,
                null
        );
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private List<String> configuredValues(String path, List<String> fallback) {
        List<String> values = plugin.getConfig().getStringList(path);
        return values.isEmpty() ? fallback : values;
    }

    // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
    private List<PdcValue> readPdcValues(PersistentDataContainer container) {
        List<PdcValue> values = new ArrayList<>();
        for (NamespacedKey key : container.getKeys()) {
            values.add(new PdcValue(
                    key,
                    readPdc(container, key, PersistentDataType.STRING),
                    readPdc(container, key, PersistentDataType.INTEGER),
                    readPdc(container, key, PersistentDataType.DOUBLE),
                    readPdc(container, key, PersistentDataType.FLOAT),
                    readPdc(container, key, PersistentDataType.LONG)
            ));
        }
        return values;
    }

    // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
    private <T> T readPdc(PersistentDataContainer container, NamespacedKey key, PersistentDataType<?, T> type) {
        try {
            return container.get(key, type);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private String itemsAdderId(ItemStack itemStack) {
        String apiId = itemsAdderIdFromApi(itemStack);
        if (!apiId.isBlank()) {
            return apiId;
        }
        if (itemStack == null || !itemStack.hasItemMeta() || itemStack.getItemMeta() == null) {
            return "";
        }
        for (PdcValue value : readPdcValues(itemStack.getItemMeta().getPersistentDataContainer())) {
            String namespace = Text.normalizeLookup(value.key().getNamespace());
            String key = Text.normalizeLookup(value.key().getKey());
            if (namespace.contains("itemsadder") && value.stringValue() != null && (key.contains("id") || key.contains("namespaced"))) {
                return value.stringValue();
            }
        }
        return "";
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private String itemsAdderIdFromApi(ItemStack itemStack) {
        if (itemStack == null || Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            return "";
        }
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method byItemStack = customStackClass.getMethod("byItemStack", ItemStack.class);
            Object customStack = byItemStack.invoke(null, itemStack);
            if (customStack == null) {
                return "";
            }
            for (String methodName : List.of("getNamespacedID", "getNamespacedId", "getId", "getID")) {
                try {
                    Method method = customStackClass.getMethod(methodName);
                    Object value = method.invoke(customStack);
                    if (value != null && !String.valueOf(value).isBlank()) {
                        return String.valueOf(value);
                    }
                } catch (ReflectiveOperationException ignored) {
                    // Try the next known ItemsAdder method name.
                }
            }
        } catch (ReflectiveOperationException exception) {
            debug("ItemsAdder id lookup failed: " + exception.getMessage());
        }
        return "";
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private boolean looksLikeYumariaFishItemsAdderId(String itemsAdderId) {
        if (itemsAdderId == null || itemsAdderId.isBlank()) {
            return false;
        }
        String normalized = Text.normalizeLookup(itemsAdderId);
        boolean namespaceMatch = configuredValues("integrations.yumaria-fishing.detection.itemsadder-namespaces", List.of("gukuan"))
                .stream()
                .map(Text::normalizeLookup)
                .anyMatch(normalized::contains);
        boolean fishMarker = List.of("fish", "fishing", "poisson", "peche", "yumaria").stream().anyMatch(normalized::contains);
        return namespaceMatch || fishMarker;
    }

    private boolean hasTextFishMarker(ItemMeta meta) {
        List<String> text = new ArrayList<>();
        if (meta.hasDisplayName()) {
            text.add(meta.getDisplayName());
        }
        if (meta.hasLore() && meta.getLore() != null) {
            text.addAll(meta.getLore());
        }
        String joined = Text.normalizeLookup(String.join(" ", text));
        if (joined.isBlank()) {
            return false;
        }
        return List.of(
                "common", "uncommon", "rare", "epic", "legendary", "mythic",
                "commun", "rare", "epique", "legendaire", "mythique",
                "bronze", "silver", "gold", "diamond", "argent", "or", "diamant",
                "poisson", "peche", "fish"
        ).stream().map(Text::normalizeLookup).anyMatch(joined::contains);
    }

    // Annotation YumariaJobs: Enregistre un element dans Bukkit ou dans le registre YumariaJobs.
    private void registerCustomCatchEvents(Plugin yumariaFishing) {
        ClassLoader loader = yumariaFishing.getClass().getClassLoader();
        for (String className : eventClassCandidates(yumariaFishing)) {
            try {
                Class<?> rawClass = Class.forName(className, false, loader);
                if (!Event.class.isAssignableFrom(rawClass)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventClass = (Class<? extends Event>) rawClass;
                EventExecutor executor = (listener, event) -> handleCustomCatchEvent(event, eventClass.getSimpleName());
                Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR, executor, plugin, false);
                registeredCustomEvents.add(className);
                debug("Registered YumariaFishing custom catch event listener: " + className);
            } catch (ClassNotFoundException ignored) {
                // Candidate class not present in this YumariaFishing build.
            } catch (RuntimeException exception) {
                debug("Could not register YumariaFishing event " + className + ": " + exception.getMessage());
            }
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private Set<String> eventClassCandidates(Plugin yumariaFishing) {
        Set<String> names = new LinkedHashSet<>();
        String basePackage = yumariaFishing.getClass().getPackageName();
        List<String> simpleNames = List.of(
                "YumariaFishCatchEvent",
                "FishCatchEvent",
                "CustomFishCatchEvent",
                "FishingRewardEvent",
                "PlayerCatchFishEvent"
        );
        List<String> packages = List.of(
                basePackage,
                basePackage + ".event",
                basePackage + ".events",
                basePackage + ".api.event",
                basePackage + ".api.events",
                "fr.yumaria.fishing.event",
                "fr.yumaria.fishing.events",
                "fr.yumaria.fishing.api.event",
                "fr.yumaria.fishing.api.events",
                "com.yumaria.fishing.event",
                "com.yumaria.fishing.events",
                "com.yumaria.fishing.api.event",
                "com.yumaria.fishing.api.events"
        );
        for (String packageName : packages) {
            for (String simpleName : simpleNames) {
                names.add(packageName + "." + simpleName);
            }
        }
        return names;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private void handleCustomCatchEvent(Event event, String eventName) {
        if (!enabled) {
            return;
        }
        if (event instanceof Cancellable cancellable && cancellable.isCancelled()) {
            debug(eventName + " ignored: event is cancelled.");
            return;
        }

        Optional<Player> player = ReflectionData.player(event);
        if (player.isEmpty()) {
            debug(eventName + " ignored: no player could be resolved.");
            return;
        }

        String catchSignal = ReflectionData.catchSignal(event);
        if (isRejectedCatchSignal(catchSignal)) {
            debug(eventName + " ignored: signal=" + catchSignal + " reason=not_confirmed_catch");
            return;
        }
        if (catchSignal.isBlank()) {
            debug(eventName + " has no state/result signal; treating registered catch event as confirmed_catch.");
        } else {
            debug(eventName + " confirmed catch signal=" + catchSignal);
        }

        FishContext context = ReflectionData.context(event, this);
        grantProgress(player.get(), context, "confirmed_catch:" + eventName);
    }

    private boolean isRejectedCatchSignal(String signal) {
        if (signal == null || signal.isBlank()) {
            return false;
        }
        String normalized = Text.normalizeLookup(signal);
        if (normalized.contains("true") && (normalized.contains("caught") || normalized.contains("success") || normalized.contains("reward"))) {
            return false;
        }
        if (normalized.contains("false") && (normalized.contains("caught") || normalized.contains("success") || normalized.contains("reward"))) {
            return true;
        }
        return normalized.contains("fishing")
                || normalized.contains("cast")
                || normalized.contains("bite")
                || normalized.contains("reelin")
                || normalized.contains("reel")
                || normalized.contains("fail")
                || normalized.contains("attempt")
                || normalized.contains("hook")
                || normalized.contains("ground");
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private void scanRecentInventoryFish(Player player, String detectionSource) {
        if (!enabled || player == null || !player.isOnline()) {
            return;
        }
        PendingCatch pendingCatch = pendingCatches.get(player.getUniqueId());
        if (pendingCatch == null) {
            debug("Inventory scan ignored: no pending confirmed catch window for player=" + player.getName()
                    + ", source=" + detectionSource);
            return;
        }
        Optional<FishContext> context = newInventoryFishContext(player, pendingCatch.beforeCounts());
        if (context.isPresent() && grantProgress(player, context.get(), detectionSource)) {
            finishPendingCatch(player.getUniqueId(), "inventory scan grant");
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private boolean grantProgress(Player player, FishContext context, String detectionSource) {
        String fingerprint = player.getUniqueId() + ":" + context.fingerprint();
        long now = System.currentTimeMillis();
        Long previous = recentGrants.get(fingerprint);
        if (previous != null && previous + DUPLICATE_GUARD_MS > now) {
            debug("Duplicate YumariaFishing catch ignored. player=" + player.getName()
                    + ", source=" + detectionSource
                    + ", fingerprint=" + context.fingerprint());
            return false;
        }
        recentGrants.put(fingerprint, now);

        double amount = calculateProgress(context);
        String configuredJobId = jobId();
        String rejectionReason = rejectionReason(player, configuredJobId);
        debug("Calculated YumariaFishing progress: player=" + player.getName()
                + ", source=" + detectionSource
                + ", speciesId=" + context.speciesId()
                + ", rarity=" + context.rarity()
                + ", quality=" + context.quality()
                + ", progress=" + amount
                + ", jobId=" + configuredJobId
                + ", accepted=" + (rejectionReason == null)
                + (rejectionReason == null ? "" : ", rejectionReason=" + rejectionReason));

        Map<String, Object> progressContext = new HashMap<>();
        context.putInto(progressContext);
        progressContext.put("integration", "yumaria_fishing");
        progressContext.put("detection_source", detectionSource);
        progressContext.put("progress_mode", progressMode());
        debug("[YumariaJobs] granting fisherman progress: player=" + player.getName()
                + ", jobId=" + configuredJobId
                + ", amount=" + amount
                + ", source=" + SOURCE);
        debug("Calling JobProgressService.addProgress(player=" + player.getName()
                + ", jobId=" + configuredJobId
                + ", amount=" + amount
                + ", source=" + SOURCE
                + ", context=" + progressContext + ")");
        progressService.addProgress(player, configuredJobId, amount, SOURCE, progressContext);
        debug("[YumariaJobs] progress granted successfully: addProgress call completed"
                + (rejectionReason == null ? "" : " (precheck warned: " + rejectionReason + ")"));
        return true;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private String rejectionReason(Player player, String configuredJobId) {
        Optional<JobDefinition> optionalJob = jobRegistry.get(configuredJobId);
        if (optionalJob.isEmpty()) {
            return "unknown job " + configuredJobId;
        }
        JobDefinition job = optionalJob.get();
        PlayerJobData data = playerDataService.getOrLoad(player).peekJob(job.id());
        if (data == null || !data.isJoined()) {
            if (!plugin.getConfig().getBoolean("progress.auto-join-on-progress", false)) {
                return "player has not joined job " + job.id();
            }
            return null;
        }
        if (!data.isActive() && !job.allowProgressWhenInactive()) {
            return "job " + job.id() + " is inactive";
        }
        if (job.actions().containsKey(SOURCE) && !job.actions().get(SOURCE).enabled()) {
            return "job action " + SOURCE + " is disabled";
        }
        return null;
    }

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
    private double calculateProgress(FishContext context) {
        if (isMasteryProgressMode()) {
            Double masteryProgress = calculateMasteryProgress(context);
            if (masteryProgress != null) {
                return masteryProgress;
            }
            debug("YumariaFishing mastery progress unavailable; falling back to configured rarity/quality progress.");
        }
        return calculateConfiguredProgress(context);
    }

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
    private double calculateConfiguredProgress(FishContext context) {
        double defaultProgress = plugin.getConfig().getDouble("integrations.yumaria-fishing.progress.default", 1.0D);
        String rarity = normalizeFactor(context.rarity());
        String quality = normalizeFactor(context.quality());
        double rarityBase = rarity.isBlank()
                ? defaultProgress
                : plugin.getConfig().getDouble("integrations.yumaria-fishing.progress.by-rarity." + rarity, defaultProgress);
        double qualityMultiplier = quality.isBlank()
                ? 1.0D
                : plugin.getConfig().getDouble("integrations.yumaria-fishing.progress.by-quality." + quality, 1.0D);
        return Math.max(0.0D, rarityBase * qualityMultiplier);
    }

    // Annotation YumariaJobs: Calcule ou interprete une valeur configurable.
    private Double calculateMasteryProgress(FishContext context) {
        Plugin fishingPlugin = yumariaFishingPlugin();
        if (!(fishingPlugin instanceof JavaPlugin javaPlugin)) {
            return null;
        }

        FileConfiguration config = javaPlugin.getConfig();
        double xp = Math.max(0.0D, config.getDouble("mastery.xp.base", 8.0D));
        xp *= configMultiplier(config, "mastery.xp.rarity-multiplier", context.rarity(), 1.0D);
        xp *= configMultiplier(config, "mastery.xp.quality-multiplier", context.quality(), 1.0D);
        xp *= configMultiplier(config, "mastery.xp.category-multiplier", context.category(), 1.0D);
        xp *= configMultiplier(config, "mastery.xp.cast-quality-multiplier", firstNonBlank(context.castQuality(), "NORMAL"), 1.0D);

        if (config.getBoolean("mastery.xp.performance.enabled", true)) {
            double performance = context.catchPerformance() == null
                    ? plugin.getConfig().getDouble("integrations.yumaria-fishing.progress.mastery-default-performance", 1.0D)
                    : context.catchPerformance();
            double minMultiplier = config.getDouble("mastery.xp.performance.min-multiplier", 0.85D);
            double maxMultiplier = Math.max(minMultiplier, config.getDouble("mastery.xp.performance.max-multiplier", 1.20D));
            xp *= minMultiplier + clamp(performance, 0.0D, 1.0D) * (maxMultiplier - minMultiplier);
        }

        if (Boolean.TRUE.equals(context.perfectCatch())) {
            xp *= Math.max(0.0D, config.getDouble("mastery.xp.perfect-catch-multiplier", 1.25D));
        }

        double min = Math.max(0.0D, config.getDouble("mastery.xp.min", 1.0D));
        double max = Math.max(min, config.getDouble("mastery.xp.max", 250.0D));
        double rawXp = round(clamp(xp, min, max), 2);
        double scale = Math.max(0.0D, plugin.getConfig().getDouble("integrations.yumaria-fishing.progress.mastery-xp-scale", 0.125D));
        double progress = round(rawXp * scale, 2);
        debug("YumariaFishing mastery progress calculated: species=" + context.speciesId()
                + ", rarity=" + context.rarity()
                + ", quality=" + context.quality()
                + ", category=" + context.category()
                + ", castQuality=" + firstNonBlank(context.castQuality(), "NORMAL")
                + ", catchPerformance=" + context.catchPerformance()
                + ", perfectCatch=" + context.perfectCatch()
                + ", rawMasteryXp=" + rawXp
                + ", scale=" + scale
                + ", progress=" + progress);
        return progress;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private double configMultiplier(FileConfiguration config, String path, String key, double fallback) {
        String normalized = key == null ? "" : key.trim();
        if (normalized.isBlank()) {
            return fallback;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        String lower = normalized.toLowerCase(Locale.ROOT);
        return Math.max(0.0D, config.getDouble(path + "." + upper, config.getDouble(path + "." + lower, fallback)));
    }

    private boolean isMasteryProgressMode() {
        String mode = Text.normalizeLookup(progressMode());
        return mode.equals("mastery")
                || mode.equals("yumariafishingmastery")
                || mode.equals("yumariafishingxp")
                || mode.equals("fishingmastery");
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private String progressMode() {
        return plugin.getConfig().getString("integrations.yumaria-fishing.progress.mode", "yumaria-fishing-mastery");
    }

    // Annotation YumariaJobs: Formate ou normalise du texte pour affichage, commandes ou recherche.
    private String normalizeFactor(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private String jobId() {
        return plugin.getConfig().getString("integrations.yumaria-fishing.job-id", "pecheur");
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private void debug(String message) {
        if (plugin.getConfig().getBoolean("integrations.yumaria-fishing.debug", false)) {
            plugin.getLogger().info("[YumariaFishingHook] " + message);
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private Plugin yumariaFishingPlugin() {
        if (yumariaFishingPlugin != null && yumariaFishingPlugin.isEnabled()) {
            return yumariaFishingPlugin;
        }
        Plugin current = Bukkit.getPluginManager().getPlugin("YumariaFishing");
        if (current != null && current.isEnabled()) {
            yumariaFishingPlugin = current;
        }
        return yumariaFishingPlugin;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private YumariaFishingState yumariaFishingState(Player player) {
        Object manager = yumariaFishingGameManager();
        if (manager == null) {
            return YumariaFishingState.unavailable();
        }
        return new YumariaFishingState(
                true,
                invokeBoolean(manager, "isActive", player),
                invokeBoolean(manager, "isHookPending", player)
        );
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private Object yumariaFishingGameManager() {
        Plugin fishingPlugin = yumariaFishingPlugin();
        if (fishingPlugin == null) {
            return null;
        }
        return invokeNoArg(fishingPlugin, "getFishingGameManager");
    }

    // Annotation YumariaJobs: Produit une copie sure pour eviter d exposer les donnees internes mutables.
    private Optional<GameSnapshot> currentGameSnapshot(Player player) {
        Object manager = yumariaFishingGameManager();
        if (manager == null) {
            return Optional.empty();
        }
        Object activeGames = readField(manager, "activeGames");
        if (!(activeGames instanceof Map<?, ?> games)) {
            return Optional.empty();
        }
        Object game = games.get(player.getUniqueId());
        if (game == null) {
            return Optional.empty();
        }

        Object species = readField(game, "species");
        int insideTicks = intValue(readField(game, "insideTicks"));
        int totalTicks = intValue(readField(game, "totalTicks"));
        double performance = totalTicks <= 0 ? 0.0D : clamp(insideTicks / (double) totalTicks, 0.0D, 1.0D);
        GameSnapshot snapshot = new GameSnapshot(
                stringValue(firstReflectionValue(species, "id", "getId", "speciesId", "getSpeciesId")),
                stringValue(firstReflectionValue(species, "rarity", "getRarity")),
                stringValue(firstReflectionValue(species, "category", "getCategory")),
                stringValue(readField(game, "castQuality")),
                performance,
                totalTicks > 0 && insideTicks == totalTicks,
                insideTicks,
                totalTicks
        );
        debug("Captured YumariaFishing game snapshot: player=" + player.getName()
                + ", species=" + snapshot.speciesId()
                + ", rarity=" + snapshot.rarity()
                + ", category=" + snapshot.category()
                + ", castQuality=" + snapshot.castQuality()
                + ", performance=" + snapshot.catchPerformance()
                + ", perfectCatch=" + snapshot.perfectCatch()
                + ", insideTicks=" + insideTicks
                + ", totalTicks=" + totalTicks);
        return Optional.of(snapshot);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private Object firstReflectionValue(Object target, String... names) {
        if (target == null) {
            return null;
        }
        for (String name : names) {
            Object value = invokeNoArg(target, name);
            if (value != null) {
                return value;
            }
            value = readField(target, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private boolean invokeBoolean(Object target, String methodName, Player player) {
        if (target == null) {
            return false;
        }
        try {
            Method method = target.getClass().getMethod(methodName, Player.class);
            method.setAccessible(true);
            Object value = method.invoke(target, player);
            return value instanceof Boolean result && result;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            debug("YumariaFishing runtime state lookup failed: method=" + methodName + ", error=" + exception.getMessage());
            return false;
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
    private Object readField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private double round(double value, int decimals) {
        double scale = Math.pow(10.0D, decimals);
        return Math.round(value * scale) / scale;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private record YumariaFishingState(boolean available, boolean active, boolean hookPending) {
        private static YumariaFishingState unavailable() {
            return new YumariaFishingState(false, false, false);
        }
    }

    private record GameSnapshot(
            String speciesId,
            String rarity,
            String category,
            String castQuality,
            Double catchPerformance,
            Boolean perfectCatch,
            int insideTicks,
            int totalTicks
    ) {
    }

    private static final class CustomCatchWatch {
        private final UUID playerId;
        private final Map<String, Integer> beforeCounts;
        private final String source;
        private final long scanIntervalTicks;
        private final long maxDurationTicks;
        private final long graceAfterInactiveTicks;
        private BukkitTask task;
        private long elapsedTicks;
        private long inactiveTicks;
        private GameSnapshot lastSnapshot;

        private CustomCatchWatch(
                UUID playerId,
                Map<String, Integer> beforeCounts,
                String source,
                long scanIntervalTicks,
                long maxDurationTicks,
                long graceAfterInactiveTicks
        ) {
            this.playerId = playerId;
            this.beforeCounts = Map.copyOf(beforeCounts);
            this.source = source;
            this.scanIntervalTicks = scanIntervalTicks;
            this.maxDurationTicks = maxDurationTicks;
            this.graceAfterInactiveTicks = graceAfterInactiveTicks;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private Map<String, Integer> beforeCounts() {
            return beforeCounts;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private long maxDurationTicks() {
            return maxDurationTicks;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private long graceAfterInactiveTicks() {
            return graceAfterInactiveTicks;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private long elapsedTicks() {
            return elapsedTicks;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private long inactiveTicks() {
            return inactiveTicks;
        }

        // Annotation YumariaJobs: Produit une copie sure pour eviter d exposer les donnees internes mutables.
        private GameSnapshot lastSnapshot() {
            return lastSnapshot;
        }

        private void setTask(BukkitTask task) {
            this.task = task;
        }

        // Annotation YumariaJobs: Produit une copie sure pour eviter d exposer les donnees internes mutables.
        private void setLastSnapshot(GameSnapshot lastSnapshot) {
            this.lastSnapshot = lastSnapshot;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private void addElapsedTicks() {
            elapsedTicks += scanIntervalTicks;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private void addInactiveTicks() {
            inactiveTicks += scanIntervalTicks;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private void resetInactiveTicks() {
            inactiveTicks = 0L;
        }

        private void cancel() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private record PendingCatch(UUID playerId, Entity caughtEntity, Map<String, Integer> beforeCounts, List<BukkitTask> tasks) {
        private PendingCatch(UUID playerId, Entity caughtEntity, Map<String, Integer> beforeCounts) {
            this(playerId, caughtEntity, Map.copyOf(beforeCounts), new ArrayList<>());
        }

        private void cancel() {
            for (BukkitTask task : tasks) {
                task.cancel();
            }
            tasks.clear();
        }
    }

    private record PdcValue(
            NamespacedKey key,
            String stringValue,
            Integer integerValue,
            Double doubleValue,
            Float floatValue,
            Long longValue
    ) {
    }

    private record DetectionReport(
            boolean matched,
            boolean namespaceMatch,
            boolean keyMatch,
            boolean itemsAdderMatch,
            boolean textMarkerMatch,
            String itemsAdderId,
            List<PdcValue> pdcValues,
            List<String> reasons,
            Optional<FishContext> context
    ) {
        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static DetectionReport empty(String reason) {
            return new DetectionReport(false, false, false, false, false, "", List.of(), List.of(reason), Optional.empty());
        }
    }

    private record FishContext(
            String speciesId,
            String rarity,
            String quality,
            String category,
            Double sizeCm,
            Double weightKg,
            Double baseValue,
            String castQuality,
            Double catchPerformance,
            Boolean perfectCatch
    ) {
        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private void putInto(Map<String, Object> target) {
            putIfPresent(target, "species", speciesId);
            putIfPresent(target, "species_id", speciesId);
            putIfPresent(target, "rarity", rarity);
            putIfPresent(target, "quality", quality);
            putIfPresent(target, "category", category);
            putIfPresent(target, "size_cm", sizeCm);
            putIfPresent(target, "weight_kg", weightKg);
            putIfPresent(target, "base_value", baseValue);
            putIfPresent(target, "cast_quality", castQuality);
            putIfPresent(target, "catch_performance", catchPerformance);
            putIfPresent(target, "perfect_catch", perfectCatch);
        }

        // Annotation YumariaJobs: Produit une copie sure pour eviter d exposer les donnees internes mutables.
        private FishContext withGameSnapshot(GameSnapshot snapshot) {
            if (snapshot == null) {
                return this;
            }
            return new FishContext(
                    firstNonBlank(speciesId, snapshot.speciesId()),
                    firstNonBlank(rarity, snapshot.rarity()),
                    quality,
                    firstNonBlank(category, snapshot.category()),
                    sizeCm,
                    weightKg,
                    baseValue,
                    firstNonBlank(castQuality, snapshot.castQuality()),
                    catchPerformance == null ? snapshot.catchPerformance() : catchPerformance,
                    perfectCatch == null ? snapshot.perfectCatch() : perfectCatch
            );
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private String fingerprint() {
            return String.join("|",
                    value(speciesId),
                    value(rarity),
                    value(quality),
                    value(category),
                    value(sizeCm),
                    value(weightKg),
                    value(baseValue),
                    value(castQuality),
                    value(catchPerformance),
                    value(perfectCatch)
            );
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static String firstNonBlank(String first, String fallback) {
            return first == null || first.isBlank() ? fallback : first;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static void putIfPresent(Map<String, Object> target, String key, Object value) {
            if (value != null && !String.valueOf(value).isBlank()) {
                target.put(key, value);
            }
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static String value(Object value) {
            return value == null ? "-" : String.valueOf(value);
        }

        // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
        private static String readString(PersistentDataContainer container, String... candidates) {
            for (NamespacedKey key : container.getKeys()) {
                if (!matches(key, candidates)) {
                    continue;
                }
                String stringValue = container.get(key, PersistentDataType.STRING);
                if (stringValue != null) {
                    return stringValue;
                }
                Object numericValue = readNumericObject(container, key);
                if (numericValue != null) {
                    return String.valueOf(numericValue);
                }
            }
            return "";
        }

        // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
        private static Double readDouble(PersistentDataContainer container, String... candidates) {
            for (NamespacedKey key : container.getKeys()) {
                if (!matches(key, candidates)) {
                    continue;
                }
                Object value = readNumericObject(container, key);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
                String stringValue = container.get(key, PersistentDataType.STRING);
                if (stringValue != null) {
                    try {
                        return Double.parseDouble(stringValue);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
            return null;
        }

        // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
        private static Object readNumericObject(PersistentDataContainer container, NamespacedKey key) {
            Double doubleValue = container.get(key, PersistentDataType.DOUBLE);
            if (doubleValue != null) {
                return doubleValue;
            }
            Float floatValue = container.get(key, PersistentDataType.FLOAT);
            if (floatValue != null) {
                return floatValue;
            }
            Integer integerValue = container.get(key, PersistentDataType.INTEGER);
            if (integerValue != null) {
                return integerValue;
            }
            Long longValue = container.get(key, PersistentDataType.LONG);
            if (longValue != null) {
                return longValue;
            }
            return null;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static boolean matches(NamespacedKey key, String... candidates) {
            String normalizedKey = Text.normalizeLookup(key.getKey());
            for (String candidate : candidates) {
                if (normalizedKey.equals(Text.normalizeLookup(candidate))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class ReflectionData {
        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private ReflectionData() {
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static Optional<Player> player(Object source) {
            Object value = firstValue(source, List.of("getPlayer", "player", "getAngler", "angler", "getFisher", "fisher"));
            if (value instanceof Player player) {
                return Optional.of(player);
            }
            if (value instanceof UUID uuid) {
                return Optional.ofNullable(Bukkit.getPlayer(uuid));
            }
            return Optional.empty();
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static FishContext context(Object event, YumariaFishingHook hook) {
            ItemStack itemStack = itemStack(event);
            Optional<FishContext> itemContext = hook.contextFromItem(itemStack);

            Object fishObject = firstValue(event, List.of("getFish", "fish", "getCaughtFish", "caughtFish", "getReward", "reward"));
            String speciesId = firstString(event, fishObject, "getSpeciesId", "speciesId", "getFishId", "fishId", "getSpecies", "species");
            String rarity = firstString(event, fishObject, "getRarity", "rarity", "getFishRarity", "fishRarity");
            String quality = firstString(event, fishObject, "getQuality", "quality", "getFishQuality", "fishQuality");
            String category = firstString(event, fishObject, "getCategory", "category", "getFishCategory", "fishCategory");
            Double sizeCm = firstDouble(event, fishObject, "getSizeCm", "sizeCm", "getSize", "size");
            Double weightKg = firstDouble(event, fishObject, "getWeightKg", "weightKg", "getWeight", "weight");
            Double baseValue = firstDouble(event, fishObject, "getBaseValue", "baseValue", "getValue", "value");

            if (itemContext.isPresent()) {
                FishContext context = itemContext.get();
                return new FishContext(
                        firstNonBlank(speciesId, context.speciesId()),
                        firstNonBlank(rarity, context.rarity()),
                        firstNonBlank(quality, context.quality()),
                        firstNonBlank(category, context.category()),
                        sizeCm == null ? context.sizeCm() : sizeCm,
                        weightKg == null ? context.weightKg() : weightKg,
                        baseValue == null ? context.baseValue() : baseValue,
                        context.castQuality(),
                        context.catchPerformance(),
                        context.perfectCatch()
                );
            }
            return new FishContext(speciesId, rarity, quality, category, sizeCm, weightKg, baseValue, "", null, null);
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static String catchSignal(Object event) {
            Object value = firstValue(event, List.of(
                    "getState",
                    "state",
                    "getStatus",
                    "status",
                    "getResult",
                    "result",
                    "getAction",
                    "action",
                    "isCaught",
                    "caught",
                    "isSuccess",
                    "success",
                    "isSuccessful",
                    "successful",
                    "hasReward",
                    "rewarded"
            ));
            return value == null ? "" : String.valueOf(value);
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static ItemStack itemStack(Object source) {
            Object value = firstValue(source, List.of("getItemStack", "itemStack", "getItem", "item", "getRewardItem", "rewardItem"));
            if (value instanceof ItemStack itemStack) {
                return itemStack;
            }
            if (value instanceof Item item) {
                return item.getItemStack();
            }
            return null;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static String firstString(Object event, Object fishObject, String... names) {
            Object value = firstValue(event, List.of(names));
            if (isBlank(value) && fishObject != null) {
                value = firstValue(fishObject, List.of(names));
            }
            return value == null ? "" : String.valueOf(value);
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static Double firstDouble(Object event, Object fishObject, String... names) {
            Object value = firstValue(event, List.of(names));
            if (value == null && fishObject != null) {
                value = firstValue(fishObject, List.of(names));
            }
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value != null) {
                try {
                    return Double.parseDouble(String.valueOf(value));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static Object firstValue(Object source, List<String> names) {
            if (source == null) {
                return null;
            }
            for (String name : names) {
                Object methodValue = invokeNoArg(source, name);
                if (methodValue != null) {
                    return methodValue;
                }
                Object fieldValue = readField(source, name);
                if (fieldValue != null) {
                    return fieldValue;
                }
            }
            return null;
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static Object invokeNoArg(Object source, String methodName) {
            try {
                Method method = source.getClass().getMethod(methodName);
                method.setAccessible(true);
                return method.invoke(source);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        // Annotation YumariaJobs: Charge les donnees depuis la configuration ou le disque.
        private static Object readField(Object source, String fieldName) {
            try {
                Field field = source.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(source);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        private static boolean isBlank(Object value) {
            return value == null || String.valueOf(value).isBlank();
        }

        // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
        private static String firstNonBlank(String first, String second) {
            return first == null || first.isBlank() ? second : first;
        }
    }
}
