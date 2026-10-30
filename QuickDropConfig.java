package com.example.mymod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Config stored in {@code config/mymod.json}.
 *
 * List entry syntax:
 *   minecraft:cobblestone  - one item (namespace "minecraft:" may be omitted)
 *   #minecraft:logs        - item tag
 *   create:*               - every item of a mod
 */
public final class QuickDropConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("mymod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mymod.json");

    private static QuickDropConfig instance = new QuickDropConfig();

    public enum Mode { WHITELIST, BLACKLIST }

    public enum Scope { INVENTORY, CONTAINER, BOTH }

    // ---- persisted fields -------------------------------------------------
    public Mode mode = Mode.WHITELIST;
    public Scope scope = Scope.BOTH;
    public boolean includeHotbar = false;
    /** Max THROW clicks sent per client tick. 64 is enough for a whole double chest + inventory in one tick. */
    public int clicksPerTick = 64;
    public boolean showMessages = true;
    /** Offset of the in-GUI buttons from the right edge / top edge of the container frame. */
    public int buttonOffsetX = 4;
    public int buttonOffsetY = 0;
    public List<String> items = new ArrayList<>();

    // ---- compiled lookup (not persisted) ----------------------------------
    private final transient Set<String> exactIds = new HashSet<>();
    private final transient Set<String> namespaces = new HashSet<>();
    private final transient List<TagKey<Item>> tags = new ArrayList<>();

    public static QuickDropConfig get() {
        return instance;
    }

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                QuickDropConfig loaded = GSON.fromJson(reader, QuickDropConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                LOGGER.error("Could not read {} - using defaults", FILE, e);
            }
        }
        instance.sanitize();
        instance.save();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Could not write {}", FILE, e);
        }
    }

    /** Call after changing plain fields (mode, scope, ...) to persist and refresh the lookup. */
    public void changed() {
        compile();
        save();
    }

    // ---- list editing -------------------------------------------------------

    public boolean add(String normalizedEntry) {
        if (items.contains(normalizedEntry)) {
            return false;
        }
        items.add(normalizedEntry);
        changed();
        return true;
    }

    public boolean remove(String entry) {
        boolean removed = items.remove(entry);
        if (removed) {
            changed();
        }
        return removed;
    }

    /** @return true if the entry was added, false if it was removed. */
    public boolean toggle(String normalizedEntry) {
        if (items.contains(normalizedEntry)) {
            remove(normalizedEntry);
            return false;
        }
        add(normalizedEntry);
        return true;
    }

    // ---- matching -----------------------------------------------------------

    public boolean shouldDrop(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        boolean listed = isListed(stack);
        return mode == Mode.WHITELIST ? listed : !listed;
    }

    private boolean isListed(ItemStack stack) {
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (exactIds.contains(key.toString()) || namespaces.contains(key.getNamespace())) {
            return true;
        }
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public static String idOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public boolean includesInventory() {
        return scope != Scope.CONTAINER;
    }

    public boolean includesContainer() {
        return scope != Scope.INVENTORY;
    }

    // ---- parsing ------------------------------------------------------------

    /** @return canonical form of an entry, or null if it is not valid. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        boolean tag = s.startsWith("#");
        if (tag) {
            s = s.substring(1);
        }
        if (s.isEmpty()) {
            return null;
        }
        if (!tag && s.endsWith(":*")) {
            String namespace = s.substring(0, s.length() - 2);
            return Identifier.tryParse(namespace + ":x") != null ? namespace + ":*" : null;
        }
        if (!s.contains(":")) {
            s = "minecraft:" + s;
        }
        Identifier id = Identifier.tryParse(s);
        if (id == null) {
            return null;
        }
        return (tag ? "#" : "") + id;
    }

    private void sanitize() {
        if (mode == null) mode = Mode.WHITELIST;
        if (scope == null) scope = Scope.BOTH;
        clicksPerTick = Math.max(1, Math.min(clicksPerTick, 1000));
        List<String> clean = new ArrayList<>();
        if (items != null) {
            for (String raw : items) {
                String normalized = normalize(raw);
                if (normalized != null && !clean.contains(normalized)) {
                    clean.add(normalized);
                }
            }
        }
        items = clean;
        compile();
    }

    private void compile() {
        exactIds.clear();
        namespaces.clear();
        tags.clear();
        for (String entry : items) {
            if (entry.startsWith("#")) {
                Identifier id = Identifier.tryParse(entry.substring(1));
                if (id != null) {
                    tags.add(TagKey.create(Registries.ITEM, id));
                }
            } else if (entry.endsWith(":*")) {
                namespaces.add(entry.substring(0, entry.length() - 2));
            } else {
                exactIds.add(entry);
            }
        }
    }
}
