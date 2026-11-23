package dinner.dev.endless_gravity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dinner.dev.endless_gravity.AtmosphereLayers;
import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.api.GravityApi;
import dinner.dev.endless_gravity.api.GravityContext;
import dinner.dev.endless_gravity.api.GravityModifierRegistry;
import dinner.dev.endless_gravity.api.GravitySource;
import dinner.dev.endless_gravity.network.ConfigSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * The {@code /endlessgravity} command, shared by both loaders (only the registration
 * differs). Subcommands:
 * <ul>
 *     <li>{@code reload} - re-read {@code dimensions.json} and re-sync config to clients.</li>
 *     <li>{@code info [entity]} - show the gravity resolved for the player or target entity.</li>
 *     <li>{@code modifiers} - list registered API {@code GravityModifier}s in execution order.</li>
 *     <li>{@code profile <dimension> info|mode|set|remove} - inspect or edit dimension profiles live.</li>
 *     <li>{@code set <key> <value>} - change a scalar setting at runtime and re-sync.</li>
 * </ul>
 */
public final class EndlessGravityCommand {

    private EndlessGravityCommand() {}

    private interface Applier {
        void apply(String value);
    }

    private interface ProfileFieldApplier {
        Config.CustomDimConfig apply(Config.CustomDimConfig current, String raw);
    }

    private static final Map<String, Applier> SETTINGS = new LinkedHashMap<>();
    private static final Map<String, ProfileFieldApplier> PROFILE_FIELDS = new LinkedHashMap<>();

    private static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String key : SETTINGS.keySet()) {
            if (key.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> PROFILE_FIELD_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String key : PROFILE_FIELDS.keySet()) {
            if (key.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String mode : List.of("atmosphere", "fixed_gravity")) {
            if (mode.startsWith(remaining)) builder.suggest(mode);
        }
        return builder.buildFuture();
    };

    static {
        // End fixed-gravity offsets (also updates active minecraft:the_end profile)
        SETTINGS.put("playerGravityOffset", dbl(-0.5, 0.5, v -> {
            Config.COMMON.playerGravityOffset.set(v);
            updateEndProfile(p -> withOffsets(p, v, p.getMobGravityOffset(), p.getItemGravityOffset(),
                    p.getArrowGravityOffset(), p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        }));
        SETTINGS.put("itemGravityOffset", dbl(-0.5, 0.5, v -> {
            Config.COMMON.itemGravityOffset.set(v);
            updateEndProfile(p -> withOffsets(p, p.getPlayerGravityOffset(), p.getMobGravityOffset(), v,
                    p.getArrowGravityOffset(), p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        }));
        SETTINGS.put("arrowGravityOffset", dbl(-0.5, 0.5, v -> {
            Config.COMMON.arrowGravityOffset.set(v);
            updateEndProfile(p -> withOffsets(p, p.getPlayerGravityOffset(), p.getMobGravityOffset(),
                    p.getItemGravityOffset(), v, p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        }));
        SETTINGS.put("thrownGravityOffset", dbl(-0.5, 0.5, v -> {
            Config.COMMON.thrownGravityOffset.set(v);
            updateEndProfile(p -> withOffsets(p, p.getPlayerGravityOffset(), p.getMobGravityOffset(),
                    p.getItemGravityOffset(), p.getArrowGravityOffset(), v, p.getBlockGravityOffset()));
        }));
        SETTINGS.put("mobGravityOffset", dbl(-0.5, 0.5, v -> {
            Config.COMMON.mobGravityOffset.set(v);
            updateEndProfile(p -> withOffsets(p, p.getPlayerGravityOffset(), v, p.getItemGravityOffset(),
                    p.getArrowGravityOffset(), p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        }));
        SETTINGS.put("blockGravityOffset", dbl(-0.5, 0.5, v -> {
            Config.COMMON.blockGravityOffset.set(v);
            updateEndProfile(p -> withOffsets(p, p.getPlayerGravityOffset(), p.getMobGravityOffset(),
                    p.getItemGravityOffset(), p.getArrowGravityOffset(), p.getThrownGravityOffset(), v));
        }));
        SETTINGS.put("particleGravityOffset", dbl(0.0, 0.5, v -> Config.COMMON.particleGravityOffset.set(v)));

        // Overworld atmosphere (also updates active minecraft:overworld profile)
        SETTINGS.put("atmosphereGravityMax", dbl(0.0, 0.5, v -> {
            Config.COMMON.atmosphereGravityMax.set(v);
            updateOverworldProfile(p -> withAtmosphere(p, v, p.getAtmosphereBaseAltitude(), p.getNoFallDamageAltitude(),
                    p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(),
                    p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving()));
        }));
        SETTINGS.put("noFallDamageAltitude", dbl(-1.0, 30000.0, v -> {
            if (v >= 64.0 && v <= 6000.0) Config.COMMON.noFallDamageAltitude.set(v);
            Double threshold = v < 0.0 ? null : v;
            updateOverworldProfile(p -> withAtmosphere(p, p.getAtmosphereGravityMax(), p.getAtmosphereBaseAltitude(),
                    threshold, p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(),
                    p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving()));
        }));
        SETTINGS.put("atmosphereMuffleGain", dbl(0.0, 1.0, v -> {
            Config.COMMON.atmosphereMuffleGain.set(v);
            updateOverworldProfile(p -> withAtmosphere(p, p.getAtmosphereGravityMax(), p.getAtmosphereBaseAltitude(),
                    p.getNoFallDamageAltitude(), v, p.getAtmosphereMuffleGainHF(),
                    p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving()));
        }));
        SETTINGS.put("atmosphereMuffleGainHF", dbl(0.0, 1.0, v -> {
            Config.COMMON.atmosphereMuffleGainHF.set(v);
            updateOverworldProfile(p -> withAtmosphere(p, p.getAtmosphereGravityMax(), p.getAtmosphereBaseAltitude(),
                    p.getNoFallDamageAltitude(), p.getAtmosphereMuffleGain(), v,
                    p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving()));
        }));

        // Audio
        SETTINGS.put("lowPassGain", dbl(0.0, 1.0, v -> Config.COMMON.lowPassGain.set(v)));
        SETTINGS.put("lowPassGainHF", dbl(0.0, 1.0, v -> Config.COMMON.lowPassGainHF.set(v)));

        // Potions
        SETTINGS.put("weightlessnessGravityOffset", dbl(0.0, 0.07, v -> Config.COMMON.weightlessnessGravityOffset.set(v)));
        SETTINGS.put("heavinessGravityOffset", dbl(0.0, 0.07, v -> Config.COMMON.heavinessGravityOffset.set(v)));

        // Toggles
        SETTINGS.put("endEntityGravity", flag(v -> Config.COMMON.endEntityGravity.set(v)));
        SETTINGS.put("enablePlayerGravity", flag(v -> {
            Config.COMMON.enablePlayerGravity.set(v);
            updateEndProfile(p -> withOffsets(p, v ? Config.COMMON.playerGravityOffset.get() : 0.0,
                    p.getMobGravityOffset(), p.getItemGravityOffset(), p.getArrowGravityOffset(),
                    p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        }));
        SETTINGS.put("enableItemGravity", flag(v -> {
            Config.COMMON.enableItemGravity.set(v);
            updateEndProfile(p -> withOffsets(p, p.getPlayerGravityOffset(), p.getMobGravityOffset(),
                    v ? Config.COMMON.itemGravityOffset.get() : 0.0, p.getArrowGravityOffset(),
                    p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        }));
        SETTINGS.put("enableArrowGravity", flag(v -> {
            Config.COMMON.enableArrowGravity.set(v);
            updateEndProfile(p -> withOffsets(p, p.getPlayerGravityOffset(), p.getMobGravityOffset(),
                    p.getItemGravityOffset(), v ? Config.COMMON.arrowGravityOffset.get() : 0.0,
                    p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        }));
        SETTINGS.put("enableThrownGravity", flag(v -> {
            Config.COMMON.enableThrownGravity.set(v);
            updateEndProfile(p -> withOffsets(p, p.getPlayerGravityOffset(), p.getMobGravityOffset(),
                    p.getItemGravityOffset(), p.getArrowGravityOffset(),
                    v ? Config.COMMON.thrownGravityOffset.get() : 0.0, p.getBlockGravityOffset()));
        }));
        SETTINGS.put("enableBlockGravity", flag(v -> {
            Config.COMMON.enableBlockGravity.set(v);
            updateEndProfile(p -> withOffsets(p, p.getPlayerGravityOffset(), p.getMobGravityOffset(),
                    p.getItemGravityOffset(), p.getArrowGravityOffset(), p.getThrownGravityOffset(),
                    v ? Config.COMMON.blockGravityOffset.get() : 0.0));
        }));
        SETTINGS.put("enableMobGravity", flag(v -> {
            Config.COMMON.enableMobGravity.set(v);
            updateEndProfile(p -> withOffsets(p, p.getPlayerGravityOffset(),
                    v ? Config.COMMON.mobGravityOffset.get() : 0.0, p.getItemGravityOffset(),
                    p.getArrowGravityOffset(), p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        }));
        SETTINGS.put("enablePotionGravity", flag(v -> Config.COMMON.enablePotionGravity.set(v)));
        SETTINGS.put("enableAtmosphericDrag", flag(v -> Config.COMMON.enableAtmosphericDrag.set(v)));
        SETTINGS.put("enableAtmosphere", flag(v -> Config.COMMON.enableAtmosphere.set(v)));
        SETTINGS.put("overworldEntityGravity", flag(v -> Config.COMMON.overworldEntityGravity.set(v)));
        SETTINGS.put("enableLowPassFilter", flag(v -> Config.COMMON.enableLowPassFilter.set(v)));

        // Fall damage (0 = vanilla, 1 = disabled, 2 = proportional)
        SETTINGS.put("fallDamageMode", num(0, 2, v -> Config.COMMON.fallDamageMode.set(v)));
        SETTINGS.put("mobFallDamageMode", num(0, 2, v -> Config.COMMON.mobFallDamageMode.set(v)));

        // Per-dimension profile fields for /endlessgravity profile <dim> set <field> <value>
        PROFILE_FIELDS.put("player", (p, raw) -> withOffsets(p, parseDouble(raw, -0.5, 0.5),
                p.getMobGravityOffset(), p.getItemGravityOffset(), p.getArrowGravityOffset(),
                p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        PROFILE_FIELDS.put("mob", (p, raw) -> withOffsets(p, p.getPlayerGravityOffset(),
                parseDouble(raw, -0.5, 0.5), p.getItemGravityOffset(), p.getArrowGravityOffset(),
                p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        PROFILE_FIELDS.put("item", (p, raw) -> withOffsets(p, p.getPlayerGravityOffset(),
                p.getMobGravityOffset(), parseDouble(raw, -0.5, 0.5), p.getArrowGravityOffset(),
                p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        PROFILE_FIELDS.put("arrow", (p, raw) -> withOffsets(p, p.getPlayerGravityOffset(),
                p.getMobGravityOffset(), p.getItemGravityOffset(), parseDouble(raw, -0.5, 0.5),
                p.getThrownGravityOffset(), p.getBlockGravityOffset()));
        PROFILE_FIELDS.put("thrown", (p, raw) -> withOffsets(p, p.getPlayerGravityOffset(),
                p.getMobGravityOffset(), p.getItemGravityOffset(), p.getArrowGravityOffset(),
                parseDouble(raw, -0.5, 0.5), p.getBlockGravityOffset()));
        PROFILE_FIELDS.put("block", (p, raw) -> withOffsets(p, p.getPlayerGravityOffset(),
                p.getMobGravityOffset(), p.getItemGravityOffset(), p.getArrowGravityOffset(),
                p.getThrownGravityOffset(), parseDouble(raw, -0.5, 0.5)));
        PROFILE_FIELDS.put("gravity_offset", (p, raw) -> withAtmosphere(p, parseDouble(raw, 0.0, 0.5),
                p.getAtmosphereBaseAltitude(), p.getNoFallDamageAltitude(),
                p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(),
                p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving()));
        PROFILE_FIELDS.put("surface_gravity_offset", (p, raw) -> withAtmosphere(p, p.getAtmosphereGravityMax(),
                p.getAtmosphereBaseAltitude(), p.getNoFallDamageAltitude(),
                p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(),
                parseDouble(raw, -0.5, 0.5), p.isAtmosphereAffectNonLiving()));
        PROFILE_FIELDS.put("start_altitude", (p, raw) -> withAtmosphere(p, p.getAtmosphereGravityMax(),
                parseDouble(raw, -2048.0, 30000.0), p.getNoFallDamageAltitude(),
                p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(),
                p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving()));
        PROFILE_FIELDS.put("no_fall_damage_above", (p, raw) -> {
            double v = parseDouble(raw, -1.0, 30000.0);
            return withAtmosphere(p, p.getAtmosphereGravityMax(), p.getAtmosphereBaseAltitude(),
                    v < 0.0 ? null : v, p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(),
                    p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving());
        });
        PROFILE_FIELDS.put("muffle_gain", (p, raw) -> {
            double v = parseDouble(raw, 0.0, 1.0);
            if (p.getMode() == Config.CustomDimConfig.Mode.FIXED_GRAVITY) {
                return withFixedAudio(p, v, p.getFixedMuffleGainHF());
            }
            return withAtmosphere(p, p.getAtmosphereGravityMax(),
                    p.getAtmosphereBaseAltitude(), p.getNoFallDamageAltitude(),
                    v, p.getAtmosphereMuffleGainHF(),
                    p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving());
        });
        PROFILE_FIELDS.put("muffle_gain_hf", (p, raw) -> {
            double v = parseDouble(raw, 0.0, 1.0);
            if (p.getMode() == Config.CustomDimConfig.Mode.FIXED_GRAVITY) {
                return withFixedAudio(p, p.getFixedMuffleGain(), v);
            }
            return withAtmosphere(p, p.getAtmosphereGravityMax(),
                    p.getAtmosphereBaseAltitude(), p.getNoFallDamageAltitude(),
                    p.getAtmosphereMuffleGain(), v,
                    p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving());
        });
        PROFILE_FIELDS.put("affect_non_living", (p, raw) -> withAtmosphere(p, p.getAtmosphereGravityMax(),
                p.getAtmosphereBaseAltitude(), p.getNoFallDamageAltitude(),
                p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(),
                p.getSurfaceGravityOffset(), parseBool(raw)));
        PROFILE_FIELDS.put("fall_damage_mode", (p, raw) -> {
            Config.CustomDimConfig.FallDamageMode mode = switch (raw.toLowerCase(Locale.ROOT)) {
                case "inherit" -> Config.CustomDimConfig.FallDamageMode.INHERIT;
                case "normal", "vanilla" -> Config.CustomDimConfig.FallDamageMode.NORMAL;
                case "proportional", "scaled" -> Config.CustomDimConfig.FallDamageMode.PROPORTIONAL;
                case "disabled", "none", "off" -> Config.CustomDimConfig.FallDamageMode.DISABLED;
                default -> throw new IllegalArgumentException("expected inherit, normal, proportional or disabled");
            };
            return new Config.CustomDimConfig(p.getDimensionId(), p.getMode(),
                    p.getPlayerGravityOffset(), p.getMobGravityOffset(), p.getItemGravityOffset(),
                    p.getArrowGravityOffset(), p.getThrownGravityOffset(), p.getBlockGravityOffset(),
                    p.getAtmosphereGravityMax(), p.getAtmosphereBaseAltitude(), p.getNoFallDamageAltitude(),
                    p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(), p.getAtmosphereLayers(),
                    p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving(), mode,
                    p.getFixedMuffleGain(), p.getFixedMuffleGainHF());
        });
    }

    private static void updateEndProfile(java.util.function.UnaryOperator<Config.CustomDimConfig> updater) {
        Config.updateDimensionProfile("minecraft:the_end", updater);
    }

    private static void updateOverworldProfile(java.util.function.UnaryOperator<Config.CustomDimConfig> updater) {
        Config.updateDimensionProfile("minecraft:overworld", updater);
    }

    private static Config.CustomDimConfig createDefaultProfile(String id, Config.CustomDimConfig.Mode mode) {
        return new Config.CustomDimConfig(id, mode,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                Config.COMMON.atmosphereGravityMax.get(), AtmosphereLayers.GRAVITY_START_ALTITUDE,
                mode == Config.CustomDimConfig.Mode.ATMOSPHERE ? Config.COMMON.noFallDamageAltitude.get() : null,
                Config.COMMON.atmosphereMuffleGain.get(), Config.COMMON.atmosphereMuffleGainHF.get(),
                List.copyOf(Config.COMMON.atmosphereLayers.get()));
    }

    private static Config.CustomDimConfig withMode(Config.CustomDimConfig p, Config.CustomDimConfig.Mode mode) {
        return new Config.CustomDimConfig(p.getDimensionId(), mode,
                p.getPlayerGravityOffset(), p.getMobGravityOffset(), p.getItemGravityOffset(),
                p.getArrowGravityOffset(), p.getThrownGravityOffset(), p.getBlockGravityOffset(),
                p.getAtmosphereGravityMax(), p.getAtmosphereBaseAltitude(), p.getNoFallDamageAltitude(),
                p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(), p.getAtmosphereLayers(),
                p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving(), p.getFallDamageMode(),
                p.getFixedMuffleGain(), p.getFixedMuffleGainHF());
    }

    private static Config.CustomDimConfig withOffsets(Config.CustomDimConfig p, double player, double mob, double item,
                                                      double arrow, double thrown, double block) {
        return new Config.CustomDimConfig(p.getDimensionId(), p.getMode(),
                player, mob, item, arrow, thrown, block,
                p.getAtmosphereGravityMax(), p.getAtmosphereBaseAltitude(), p.getNoFallDamageAltitude(),
                p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(), p.getAtmosphereLayers(),
                p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving(), p.getFallDamageMode(),
                p.getFixedMuffleGain(), p.getFixedMuffleGainHF());
    }

    private static Config.CustomDimConfig withAtmosphere(Config.CustomDimConfig p, double maxGravity, double baseAlt,
                                                         Double noFall, double gain, double gainHF,
                                                         double surfaceGravity, boolean affectNonLiving) {
        return new Config.CustomDimConfig(p.getDimensionId(), p.getMode(),
                p.getPlayerGravityOffset(), p.getMobGravityOffset(), p.getItemGravityOffset(),
                p.getArrowGravityOffset(), p.getThrownGravityOffset(), p.getBlockGravityOffset(),
                maxGravity, baseAlt, noFall, gain, gainHF, p.getAtmosphereLayers(),
                surfaceGravity, affectNonLiving, p.getFallDamageMode(),
                p.getFixedMuffleGain(), p.getFixedMuffleGainHF());
    }

    private static Config.CustomDimConfig withFixedAudio(Config.CustomDimConfig p, double fixedGain, double fixedGainHF) {
        return new Config.CustomDimConfig(p.getDimensionId(), p.getMode(),
                p.getPlayerGravityOffset(), p.getMobGravityOffset(), p.getItemGravityOffset(),
                p.getArrowGravityOffset(), p.getThrownGravityOffset(), p.getBlockGravityOffset(),
                p.getAtmosphereGravityMax(), p.getAtmosphereBaseAltitude(), p.getNoFallDamageAltitude(),
                p.getAtmosphereMuffleGain(), p.getAtmosphereMuffleGainHF(), p.getAtmosphereLayers(),
                p.getSurfaceGravityOffset(), p.isAtmosphereAffectNonLiving(), p.getFallDamageMode(),
                fixedGain, fixedGainHF);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("endlessgravity")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("info")
                        .executes(context -> info(context.getSource(), null))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> info(context.getSource(),
                                        EntityArgument.getEntity(context, "target")))))
                .then(Commands.literal("modifiers")
                        .executes(context -> modifiers(context.getSource())))
                .then(Commands.literal("profile")
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .then(Commands.literal("info")
                                        .executes(context -> profileInfo(context.getSource(),
                                                DimensionArgument.getDimension(context, "dimension"))))
                                .then(Commands.literal("mode")
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests(MODE_SUGGESTIONS)
                                                .executes(context -> profileMode(context.getSource(),
                                                        DimensionArgument.getDimension(context, "dimension"),
                                                        StringArgumentType.getString(context, "mode")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("field", StringArgumentType.word())
                                                .suggests(PROFILE_FIELD_SUGGESTIONS)
                                                .then(Commands.argument("value", StringArgumentType.word())
                                                        .executes(context -> profileSet(context.getSource(),
                                                                DimensionArgument.getDimension(context, "dimension"),
                                                                StringArgumentType.getString(context, "field"),
                                                                StringArgumentType.getString(context, "value"))))))
                                .then(Commands.literal("remove")
                                        .executes(context -> profileRemove(context.getSource(),
                                                DimensionArgument.getDimension(context, "dimension"))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(context -> set(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "key"),
                                                StringArgumentType.getString(context, "value")))))));
    }

    private static int reload(CommandSourceStack source) {
        Config.reloadCustomDimensions();
        ConfigSync.broadcast(source.getServer());
        source.sendSuccess(() -> Component.literal(
                "[Endless Gravity] Reloaded dimensions.json and re-sent config to clients."), true);
        return 1;
    }

    private static int info(CommandSourceStack source, Entity target) {
        Entity entity = target;
        if (entity == null) {
            try {
                entity = source.getPlayerOrException();
            } catch (Exception e) {
                source.sendFailure(Component.literal("[Endless Gravity] Specify a target entity or run as a player."));
                return 0;
            }
        }
        GravityContext context = GravityApi.getGravityContext(entity);
        double offset = context != null && context.applicable() ? context.offset() : 0.0;
        double base = context != null ? context.baseGravity() : entity.getGravity();
        double ratio = base > 0.0 ? Math.max(0.0, (base - offset) / base) : 1.0;
        GravitySource origin = context != null ? context.source() : GravitySource.NONE;
        double realY = GravityApi.getRealY(entity);
        double pressure = GravityApi.getPressure(entity.level(), realY);
        double progress = GravityApi.getAtmosphereProgress(entity.level(), realY);
        final String name = entity.getName().getString();
        final ResourceLocation dim = entity.level().dimension().location();
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[Endless Gravity] %s @ %s | source=%s offset=%+.4f (%.2fg) | realY=%.1f pressure=%.3f progress=%.3f",
                name, dim, origin, offset, ratio, realY, pressure, progress)), false);
        return 1;
    }

    private static int profileInfo(CommandSourceStack source, ServerLevel dimension) {
        String id = dimension.dimension().location().toString();
        Config.CustomDimConfig profile = Config.getCustomDimConfig(dimension.dimension().location()).orElse(null);
        if (profile == null) {
            source.sendSuccess(() -> Component.literal("[Endless Gravity] No profile configured for " + id + " (vanilla gravity)."), false);
            return 1;
        }
        String summary = profile.getMode() == Config.CustomDimConfig.Mode.FIXED_GRAVITY
                ? String.format(Locale.ROOT,
                "[Endless Gravity] %s [fixed_gravity] player=%+.4f mob=%+.4f item=%+.4f arrow=%+.4f thrown=%+.4f block=%+.4f fall=%s",
                id, profile.getPlayerGravityOffset(), profile.getMobGravityOffset(),
                profile.getItemGravityOffset(), profile.getArrowGravityOffset(),
                profile.getThrownGravityOffset(), profile.getBlockGravityOffset(),
                profile.getFallDamageMode().name().toLowerCase(Locale.ROOT))
                : String.format(Locale.ROOT,
                "[Endless Gravity] %s [atmosphere] surface=%+.4f max=%+.4f startY=%.0f noFallY=%s nonLiving=%s fall=%s layers=%d",
                id, profile.getSurfaceGravityOffset(), profile.getAtmosphereGravityMax(),
                profile.getAtmosphereBaseAltitude(),
                profile.getNoFallDamageAltitude() == null ? "none" : String.format(Locale.ROOT, "%.0f", profile.getNoFallDamageAltitude()),
                profile.isAtmosphereAffectNonLiving(),
                profile.getFallDamageMode().name().toLowerCase(Locale.ROOT),
                profile.getParsedAtmosphereLayers().size());
        source.sendSuccess(() -> Component.literal(summary), false);
        return 1;
    }

    private static int profileMode(CommandSourceStack source, ServerLevel dimension, String rawMode) {
        Config.CustomDimConfig.Mode mode = switch (rawMode.toLowerCase(Locale.ROOT)) {
            case "atmosphere" -> Config.CustomDimConfig.Mode.ATMOSPHERE;
            case "fixed_gravity", "fixed-gravity", "gravity" -> Config.CustomDimConfig.Mode.FIXED_GRAVITY;
            default -> null;
        };
        if (mode == null) {
            source.sendFailure(Component.literal("[Endless Gravity] Invalid mode '" + rawMode + "'. Use atmosphere or fixed_gravity."));
            return 0;
        }
        String id = dimension.dimension().location().toString();
        Config.CustomDimConfig existing = Config.getCustomDimConfig(dimension.dimension().location())
                .orElseGet(() -> createDefaultProfile(id, mode));
        Config.putDimensionProfile(withMode(existing, mode));
        ConfigSync.broadcast(source.getServer());
        source.sendSuccess(() -> Component.literal("[Endless Gravity] " + id + " mode = " + mode.name().toLowerCase(Locale.ROOT)), true);
        return 1;
    }

    private static int profileSet(CommandSourceStack source, ServerLevel dimension, String field, String rawValue) {
        ProfileFieldApplier applier = PROFILE_FIELDS.get(field);
        if (applier == null) {
            source.sendFailure(Component.literal("[Endless Gravity] Unknown profile field '" + field
                    + "'. Valid: " + String.join(", ", PROFILE_FIELDS.keySet())));
            return 0;
        }
        String id = dimension.dimension().location().toString();
        Config.CustomDimConfig existing = Config.getCustomDimConfig(dimension.dimension().location())
                .orElseGet(() -> createDefaultProfile(id, Config.CustomDimConfig.Mode.FIXED_GRAVITY));
        try {
            Config.putDimensionProfile(applier.apply(existing, rawValue));
        } catch (RuntimeException e) {
            source.sendFailure(Component.literal("[Endless Gravity] Invalid value '" + rawValue
                    + "' for '" + field + "': " + e.getMessage()));
            return 0;
        }
        ConfigSync.broadcast(source.getServer());
        source.sendSuccess(() -> Component.literal("[Endless Gravity] " + id + " " + field + " = " + rawValue), true);
        return 1;
    }

    private static int profileRemove(CommandSourceStack source, ServerLevel dimension) {
        String id = dimension.dimension().location().toString();
        if (!Config.removeDimensionProfile(id)) {
            source.sendFailure(Component.literal("[Endless Gravity] No user profile exists for " + id + "."));
            return 0;
        }
        ConfigSync.broadcast(source.getServer());
        source.sendSuccess(() -> Component.literal("[Endless Gravity] Removed user profile for " + id + "."), true);
        return 1;
    }

    private static int modifiers(CommandSourceStack source) {
        List<GravityModifierRegistry.Entry> entries = GravityApi.getRegisteredModifiers();
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("[Endless Gravity] No global GravityModifiers registered."), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("[Endless Gravity] Registered modifiers (" + entries.size() + "):");
        for (GravityModifierRegistry.Entry entry : entries) {
            sb.append("\n  - ").append(entry.id()).append(" (priority=").append(entry.priority()).append(")");
        }
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return entries.size();
    }

    private static int set(CommandSourceStack source, String key, String rawValue) {
        Applier applier = SETTINGS.get(key);
        if (applier == null) {
            source.sendFailure(Component.literal("[Endless Gravity] Unknown setting '" + key
                    + "'. Valid: " + String.join(", ", SETTINGS.keySet())));
            return 0;
        }
        try {
            applier.apply(rawValue);
        } catch (RuntimeException e) {
            source.sendFailure(Component.literal(
                    "[Endless Gravity] Invalid value '" + rawValue + "' for '" + key + "': " + e.getMessage()));
            return 0;
        }
        Config.persist();
        ConfigSync.broadcast(source.getServer());
        source.sendSuccess(() -> Component.literal("[Endless Gravity] " + key + " = " + rawValue), true);
        return 1;
    }

    private static double parseDouble(String raw, double min, double max) {
        double v = Double.parseDouble(raw);
        if (!Double.isFinite(v) || v < min || v > max) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "expected finite number in [%.4f, %.4f]", min, max));
        }
        return v;
    }

    private static boolean parseBool(String raw) {
        if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("expected true or false");
        }
        return Boolean.parseBoolean(raw);
    }

    private static Applier dbl(double min, double max, DoubleConsumer setter) {
        return raw -> setter.accept(parseDouble(raw, min, max));
    }

    private static Applier flag(Consumer<Boolean> setter) {
        return raw -> setter.accept(parseBool(raw));
    }

    private static Applier num(int min, int max, Consumer<Integer> setter) {
        return raw -> {
            int v = Integer.parseInt(raw);
            if (v < min || v > max) {
                throw new IllegalArgumentException("expected integer in [" + min + ", " + max + "]");
            }
            setter.accept(v);
        };
    }
}
