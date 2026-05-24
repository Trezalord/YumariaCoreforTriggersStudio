package fr.yumaria.jobs.hook;

import fr.yumaria.jobs.YumariaJobsPlugin;
import fr.yumaria.jobs.config.JobRegistry;
import fr.yumaria.jobs.data.PlayerDataService;
import fr.yumaria.jobs.data.PlayerJobData;
import fr.yumaria.jobs.job.JobDefinition;
import fr.yumaria.jobs.progress.JobProgressService;
import fr.yumaria.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
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

public final class YumariaFishingHook implements Listener {
    private static final String SOURCE = "yumaria_fish_catch";
    private static final long DUPLICATE_GUARD_MS = 750L;

    private final YumariaJobsPlugin plugin;
    private final JobRegistry jobRegistry;
    private final PlayerDataService playerDataService;
    private final JobProgressService progressService;
    private final Map<String, Long> recentGrants = new ConcurrentHashMap<>();
    private final Map<UUID, PendingCatch> pendingCatches = new ConcurrentHashMap<>();
    private final List<String> registeredCustomEvents = new ArrayList<>();
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
            debug("YumariaFishing hook disabled: plugin is not enabled.");
            return;
        }

        enabled = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        fallbackRegistered = true;
        registerCustomCatchEvents(yumariaFishing);
        debug("YumariaFishing hook enabled. fallbackRegistered=" + fallbackRegistered
                + ", customEvents=" + registeredCustomEvents
                + ", jobId=" + jobId());
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        registeredCustomEvents.clear();
        fallbackRegistered = false;
        enabled = false;
        recentGrants.clear();
        for (PendingCatch pendingCatch : pendingCatches.values()) {
            pendingCatch.cancel();
        }
        pendingCatches.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPluginPresent() {
        return pluginPresent;
    }

    public String status() {
        return "enabled=" + enabled
                + ", pluginPresent=" + pluginPresent
                + ", fallbackRegistered=" + fallbackRegistered
                + ", customEvents=" + (registeredCustomEvents.isEmpty() ? "-" : String.join(",", registeredCustomEvents));
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
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

    private Optional<FishContext> contextFromItem(ItemStack itemStack) {
        return inspectItem(itemStack).context();
    }

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

    private Optional<FishContext> contextFromCaughtEntity(Entity caughtEntity) {
        if (!(caughtEntity instanceof Item item)) {
            return Optional.empty();
        }
        return contextFromItem(item.getItemStack());
    }

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

    private void finishPendingCatch(UUID uuid, String reason) {
        PendingCatch pendingCatch = pendingCatches.remove(uuid);
        if (pendingCatch == null) {
            return;
        }
        pendingCatch.cancel();
        debug("Pending confirmed catch finished: uuid=" + uuid + ", reason=" + reason);
    }

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
                FishContext.readDouble(container, "baseValue", "base_value", "base", "value")
        );
    }

    private List<String> configuredValues(String path, List<String> fallback) {
        List<String> values = plugin.getConfig().getStringList(path);
        return values.isEmpty() ? fallback : values;
    }

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

    private <T> T readPdc(PersistentDataContainer container, NamespacedKey key, PersistentDataType<?, T> type) {
        try {
            return container.get(key, type);
        } catch (RuntimeException exception) {
            return null;
        }
    }

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

    private double calculateProgress(FishContext context) {
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

    private String normalizeFactor(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String jobId() {
        return plugin.getConfig().getString("integrations.yumaria-fishing.job-id", "pecheur");
    }

    private void debug(String message) {
        if (plugin.getConfig().getBoolean("integrations.yumaria-fishing.debug", false)) {
            plugin.getLogger().info("[YumariaFishingHook] " + message);
        }
    }

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
            Double baseValue
    ) {
        private void putInto(Map<String, Object> target) {
            putIfPresent(target, "species", speciesId);
            putIfPresent(target, "species_id", speciesId);
            putIfPresent(target, "rarity", rarity);
            putIfPresent(target, "quality", quality);
            putIfPresent(target, "category", category);
            putIfPresent(target, "size_cm", sizeCm);
            putIfPresent(target, "weight_kg", weightKg);
            putIfPresent(target, "base_value", baseValue);
        }

        private String fingerprint() {
            return String.join("|",
                    value(speciesId),
                    value(rarity),
                    value(quality),
                    value(category),
                    value(sizeCm),
                    value(weightKg),
                    value(baseValue)
            );
        }

        private static void putIfPresent(Map<String, Object> target, String key, Object value) {
            if (value != null && !String.valueOf(value).isBlank()) {
                target.put(key, value);
            }
        }

        private static String value(Object value) {
            return value == null ? "-" : String.valueOf(value);
        }

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
        private ReflectionData() {
        }

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
                        baseValue == null ? context.baseValue() : baseValue
                );
            }
            return new FishContext(speciesId, rarity, quality, category, sizeCm, weightKg, baseValue);
        }

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

        private static String firstString(Object event, Object fishObject, String... names) {
            Object value = firstValue(event, List.of(names));
            if (isBlank(value) && fishObject != null) {
                value = firstValue(fishObject, List.of(names));
            }
            return value == null ? "" : String.valueOf(value);
        }

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

        private static Object invokeNoArg(Object source, String methodName) {
            try {
                Method method = source.getClass().getMethod(methodName);
                method.setAccessible(true);
                return method.invoke(source);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

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

        private static String firstNonBlank(String first, String second) {
            return first == null || first.isBlank() ? second : first;
        }
    }
}
