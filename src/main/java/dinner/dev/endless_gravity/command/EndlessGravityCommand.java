package dinner.dev.endless_gravity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.api.GravityApi;
import dinner.dev.endless_gravity.api.GravityContext;
import dinner.dev.endless_gravity.api.GravitySource;
import dinner.dev.endless_gravity.network.ConfigSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * The {@code /endlessgravity} command, shared by both loaders (only the registration
 * differs). Subcommands:
 * <ul>
 *     <li>{@code reload} - re-read {@code dimensions.json} and re-sync config to clients.</li>
 *     <li>{@code info} - show the gravity resolved for the executing player right now.</li>
 *     <li>{@code set <key> <value>} - change a scalar setting at runtime and re-sync.</li>
 * </ul>
 */
public final class EndlessGravityCommand {

    private EndlessGravityCommand() {}

    private interface Applier {
        void apply(String value);
    }

    private static final Map<String, Applier> SETTINGS = new LinkedHashMap<>();

    private static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String key : SETTINGS.keySet()) {
            if (key.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    };

    static {
        // Gravity offsets
        SETTINGS.put("playerGravityOffset", dbl(v -> Config.COMMON.playerGravityOffset.set(v)));
        SETTINGS.put("itemGravityOffset", dbl(v -> Config.COMMON.itemGravityOffset.set(v)));
        SETTINGS.put("arrowGravityOffset", dbl(v -> Config.COMMON.arrowGravityOffset.set(v)));
        SETTINGS.put("thrownGravityOffset", dbl(v -> Config.COMMON.thrownGravityOffset.set(v)));
        SETTINGS.put("mobGravityOffset", dbl(v -> Config.COMMON.mobGravityOffset.set(v)));
        SETTINGS.put("blockGravityOffset", dbl(v -> Config.COMMON.blockGravityOffset.set(v)));
        SETTINGS.put("particleGravityOffset", dbl(v -> Config.COMMON.particleGravityOffset.set(v)));

        // Atmosphere
        SETTINGS.put("atmosphereGravityMax", dbl(v -> Config.COMMON.atmosphereGravityMax.set(v)));
        SETTINGS.put("noFallDamageAltitude", dbl(v -> Config.COMMON.noFallDamageAltitude.set(v)));
        SETTINGS.put("atmosphereMuffleGain", dbl(v -> Config.COMMON.atmosphereMuffleGain.set(v)));
        SETTINGS.put("atmosphereMuffleGainHF", dbl(v -> Config.COMMON.atmosphereMuffleGainHF.set(v)));

        // Audio
        SETTINGS.put("lowPassGain", dbl(v -> Config.COMMON.lowPassGain.set(v)));
        SETTINGS.put("lowPassGainHF", dbl(v -> Config.COMMON.lowPassGainHF.set(v)));

        // Potions
        SETTINGS.put("weightlessnessGravityOffset", dbl(v -> Config.COMMON.weightlessnessGravityOffset.set(v)));
        SETTINGS.put("heavinessGravityOffset", dbl(v -> Config.COMMON.heavinessGravityOffset.set(v)));

        // Toggles
        SETTINGS.put("endEntityGravity", flag(v -> Config.COMMON.endEntityGravity.set(v)));
        SETTINGS.put("enablePlayerGravity", flag(v -> Config.COMMON.enablePlayerGravity.set(v)));
        SETTINGS.put("enableItemGravity", flag(v -> Config.COMMON.enableItemGravity.set(v)));
        SETTINGS.put("enableArrowGravity", flag(v -> Config.COMMON.enableArrowGravity.set(v)));
        SETTINGS.put("enableThrownGravity", flag(v -> Config.COMMON.enableThrownGravity.set(v)));
        SETTINGS.put("enableBlockGravity", flag(v -> Config.COMMON.enableBlockGravity.set(v)));
        SETTINGS.put("enableMobGravity", flag(v -> Config.COMMON.enableMobGravity.set(v)));
        SETTINGS.put("enablePotionGravity", flag(v -> Config.COMMON.enablePotionGravity.set(v)));
        SETTINGS.put("enableAtmosphere", flag(v -> Config.COMMON.enableAtmosphere.set(v)));
        SETTINGS.put("overworldEntityGravity", flag(v -> Config.COMMON.overworldEntityGravity.set(v)));
        SETTINGS.put("enableLowPassFilter", flag(v -> Config.COMMON.enableLowPassFilter.set(v)));

        // Fall damage (0 = vanilla, 1 = disabled)
        SETTINGS.put("fallDamageMode", num(v -> Config.COMMON.fallDamageMode.set(v)));
        SETTINGS.put("mobFallDamageMode", num(v -> Config.COMMON.mobFallDamageMode.set(v)));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("endlessgravity")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("info")
                        .executes(context -> info(context.getSource())))
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

    private static int info(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("[Endless Gravity] /endlessgravity info must be run by a player."));
            return 0;
        }
        GravityContext context = GravityApi.getGravityContext(player);
        double offset = context != null ? context.offset() : 0.0;
        GravitySource origin = context != null ? context.source() : GravitySource.NONE;
        double realY = GravityApi.getRealY(player);
        double pressure = GravityApi.getPressure(realY);
        double progress = GravityApi.getAtmosphereProgress(realY);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[Endless Gravity] %s | source=%s offset=%.4f | realY=%.1f pressure=%.3f progress=%.3f",
                player.level().dimension().location(), origin, offset, realY, pressure, progress)), false);
        return 1;
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
                    "[Endless Gravity] Invalid value '" + rawValue + "' for '" + key + "'."));
            return 0;
        }
        Config.persist();
        ConfigSync.broadcast(source.getServer());
        source.sendSuccess(() -> Component.literal("[Endless Gravity] " + key + " = " + rawValue), true);
        return 1;
    }

    private static Applier dbl(DoubleConsumer setter) {
        return raw -> setter.accept(Double.parseDouble(raw));
    }

    private static Applier flag(Consumer<Boolean> setter) {
        return raw -> {
            if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException("expected true or false");
            }
            setter.accept(Boolean.parseBoolean(raw));
        };
    }

    private static Applier num(Consumer<Integer> setter) {
        return raw -> setter.accept(Integer.parseInt(raw));
    }
}
