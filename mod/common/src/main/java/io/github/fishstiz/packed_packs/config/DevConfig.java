package io.github.fishstiz.packed_packs.config;

import io.github.fishstiz.fidgetz.util.lang.CollectionsUtil;
import io.github.fishstiz.packed_packs.PackedPacks;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class DevConfig implements Serializable {
    private static final String FILENAME = "config.meta.json";
    private static final DevConfig INSTANCE = JsonLoader.loadJsonOrDefault(getPath(), DevConfig.class, DevConfig::new);
    private final ResourcePacks resourcepacks = new ResourcePacks();
    private final DataPacks datapacks = new DataPacks();

    private static Path getPath() {
        return PackedPacks.getConfigDir().resolve(FILENAME);
    }

    public static DevConfig get() {
        return INSTANCE;
    }

    public static Packs packs(PackType packType) {
        return INSTANCE.get(packType);
    }

    public Packs get(PackType packType) {
        return switch (packType) {
            case CLIENT_RESOURCES -> this.resourcepacks;
            case SERVER_DATA -> this.datapacks;
        };
    }

    public DataPacks getDatapacks() {
        return this.datapacks;
    }

    public ResourcePacks getResourcepacks() {
        return this.resourcepacks;
    }

    public void save() {
        JsonLoader.saveJson(this, getPath());
    }

    private DevConfig() {
    }

    public abstract static sealed class Packs implements Serializable {
        private static final String REGEX_PREFIX = "regex:";
        private final Map<String, String> aliases = new Object2ObjectLinkedOpenHashMap<>();
        private transient @Nullable Map<Pattern, String> aliasPatterns;
        private @Nullable String defaultProfile;

        public abstract PackType packType();

        public static boolean isRegexPrefixed(String alias) {
            return alias.startsWith(REGEX_PREFIX);
        }

        public static Pattern getRegexPrefixPattern() {
            return Pattern.compile("^" + Pattern.quote(REGEX_PREFIX));
        }

        public boolean hasAliases() {
            return !this.aliases.isEmpty();
        }

        public List<String> getAliases(String packId) {
            return CollectionsUtil.reverseLookup(packId, this.aliases);
        }

        public boolean hasAlias(String packId) {
            return this.aliases.containsValue(packId);
        }

        public boolean isAlias(String packId) {
            return this.aliases.containsKey(packId);
        }

        public void putAlias(String aliasId, String canonicalId) {
            this.aliases.put(aliasId, canonicalId);
            this.aliases.remove(canonicalId);
        }

        public @Nullable String resolveCanonicalId(String aliasId) {
            String exact = this.aliases.get(aliasId);
            if (exact != null) return exact;

            Map<Pattern, String> patternMap = this.getAliasPatterns();
            if (patternMap == null) return null;

            for (Map.Entry<Pattern, String> patternEntry : patternMap.entrySet()) {
                if (patternEntry.getKey().matcher(aliasId).matches()) {
                    return patternEntry.getValue();
                }
            }

            return null;
        }

        private @Nullable Map<Pattern, String> getAliasPatterns() {
            if (this.aliases.isEmpty()) return null;

            if (this.aliasPatterns == null) {
                Map<Pattern, String> patterns = new Object2ObjectOpenHashMap<>();
                Pattern regexPrefixPattern = getRegexPrefixPattern();

                for (Map.Entry<String, String> aliasEntry : this.aliases.entrySet()) {
                    String alias = aliasEntry.getKey();
                    String canonicalId = aliasEntry.getValue();
                    if (isRegexPrefixed(alias)) {
                        try {
                            patterns.put(Pattern.compile(alias.replaceFirst(regexPrefixPattern.pattern(), "")), canonicalId);
                        } catch (PatternSyntaxException e) {
                            PackedPacks.LOGGER.error("[packed_packs] Invalid regex syntax '{}' for id '{}'", alias, canonicalId, e);
                        }
                    }
                }
                this.aliasPatterns = Collections.unmodifiableMap(patterns);
            }

            return this.aliasPatterns;
        }

        public void setAliases(String packId, List<String> aliases) {
            CollectionsUtil.updateReverseMapping(this.aliases, packId, aliases);
            this.aliasPatterns = null;
        }

        @Nullable String getDefaultProfile() {
            return this.defaultProfile;
        }

        void setDefaultProfile(@Nullable Profile profile) {
            this.defaultProfile = profile == null ? null : profile.getId();
        }
    }

    public static final class ResourcePacks extends Packs {
        @Override
        public PackType packType() {
            return PackType.CLIENT_RESOURCES;
        }
    }

    public static final class DataPacks extends Packs {
        @Override
        public PackType packType() {
            return PackType.SERVER_DATA;
        }
    }
}
