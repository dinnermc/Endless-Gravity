package dinner.dev.endless_gravity;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EndlessGravityConfigScreen {

    private static final int TEXT_WHITE = 0xFFFFFF;
    private static final int TEXT_GRAY = 0xAAAAAA;
    private static final int TEXT_BLUE = 0x66CCFF;
    private static final int TEXT_GREEN = 0x55FF55;
    private static final int TEXT_RED = 0xFF5555;
    private static final int TEXT_AMBER = 0xFFAA00;

    private EndlessGravityConfigScreen() {}

    public static Screen create(Screen parent) {
        return new Factory(parent).build();
    }

    private enum FallDamageMode {
        NORMAL, DISABLED, VELOCITY;

        static FallDamageMode fromInt(int value) {
            return switch (value) {
                case 0 -> NORMAL;
                case 2 -> VELOCITY;
                default -> DISABLED;
            };
        }
    }

    private static final class Factory {
        private final Screen parent;
        private final boolean sableLoaded;
        private final List<AtmosphereLayers.Layer> layers = new ArrayList<>();

        Factory(Screen parent) {
            this.parent = parent;
            this.sableLoaded = ModList.get().isLoaded("sable");
            loadLayers();
        }

        Screen build() {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("endless_gravity.config.title"))
                    .setDoesConfirmSave(false)
                    .setSavingRunnable(this::finishSave);

            ConfigEntryBuilder entry = builder.entryBuilder();

            ConfigCategory all = builder.getOrCreateCategory(text("endless_gravity.config.section.all"));
            buildEnd(builder, entry, all);
            buildOverworld(builder, entry, all);
            buildGeneral(builder, entry, all);
            return builder.build();
        }

        private void buildEnd(ConfigBuilder builder, ConfigEntryBuilder entry, ConfigCategory all) {
            ConfigCategory cat = category(builder, "endless_gravity.config.section.end");
            SubCategoryBuilder allEnd = group(entry, "endless_gravity.config.section.end");

            SubCategoryBuilder gravity = group(entry, "endless_gravity.config.group.gravity");
            addToggle(gravity, entry, "endless_gravity.config.endEntityGravity", Config.COMMON.endEntityGravity, true);
            addDouble(gravity, entry, "endless_gravity.config.playerGravityOffset", Config.COMMON.playerGravityOffset,
                    0.0, 0.07, 0.055, TEXT_WHITE);
            addDouble(gravity, entry, "endless_gravity.config.itemGravityOffset", Config.COMMON.itemGravityOffset,
                    0.0, 0.035, 0.025, TEXT_GRAY);
            addDouble(gravity, entry, "endless_gravity.config.arrowGravityOffset", Config.COMMON.arrowGravityOffset,
                    0.0, 0.04, 0.03, TEXT_GRAY);
            addDouble(gravity, entry, "endless_gravity.config.thrownGravityOffset", Config.COMMON.thrownGravityOffset,
                    0.0, 0.025, 0.018, TEXT_GRAY);
            addToggle(gravity, entry, "endless_gravity.config.enableBlockGravity", Config.COMMON.enableBlockGravity, true);
            addDouble(gravity, entry, "endless_gravity.config.blockGravityOffset", Config.COMMON.blockGravityOffset,
                    0.0, 0.1, 0.035, TEXT_GRAY);
            cat.addEntry(gravity.build());
            allEnd.add(gravity.build());

            SubCategoryBuilder audio = group(entry, "endless_gravity.config.group.audio");
            addToggle(audio, entry, "endless_gravity.config.enableLowPassFilter", Config.COMMON.enableLowPassFilter, true);
            addDouble(audio, entry, "endless_gravity.config.lowPassGain", Config.COMMON.lowPassGain,
                    0.0, 1.0, 0.1, TEXT_WHITE);
            addDouble(audio, entry, "endless_gravity.config.lowPassGainHF", Config.COMMON.lowPassGainHF,
                    0.0, 1.0, 0.05, TEXT_GRAY);
            cat.addEntry(audio.build());
            allEnd.add(audio.build());

            if (sableLoaded) {
                SubCategoryBuilder sable = group(entry, "endless_gravity.config.group.sable_end");
                sable.add(entry.startTextDescription(colored("endless_gravity.config.restartWarning", TEXT_AMBER)).build());
                addToggle(sable, entry, "endless_gravity.config.endSableGravity", Config.COMMON.endSableGravity, true);
                addDouble(sable, entry, "endless_gravity.config.sableGravityY", Config.COMMON.sableGravityY,
                        -20.0, 0.0, -4.0, TEXT_WHITE);
                addDouble(sable, entry, "endless_gravity.config.sablePressure", Config.COMMON.sablePressure,
                        0.0, 10.0, 0.0, TEXT_GRAY);
                addDouble(sable, entry, "endless_gravity.config.sableDrag", Config.COMMON.sableDrag,
                        0.0, 10.0, 0.05, TEXT_GRAY);
                addIntSlider(sable, entry, "endless_gravity.config.sableDatapackPriority", Config.COMMON.sableDatapackPriority,
                        1, 9999, 9999, TEXT_GRAY);
                cat.addEntry(sable.build());
                allEnd.add(sable.build());
            }

            all.addEntry(allEnd.build());
        }

        private void buildOverworld(ConfigBuilder builder, ConfigEntryBuilder entry, ConfigCategory all) {
            ConfigCategory cat = category(builder, "endless_gravity.config.section.overworld");
            SubCategoryBuilder allOverworld = group(entry, "endless_gravity.config.section.overworld");

            SubCategoryBuilder atmosphere = group(entry, "endless_gravity.config.group.atmosphere");
            addToggle(atmosphere, entry, "endless_gravity.config.enableAtmosphere", Config.COMMON.enableAtmosphere, true);
            addToggle(atmosphere, entry, "endless_gravity.config.overworldEntityGravity", Config.COMMON.overworldEntityGravity, true);
            addDouble(atmosphere, entry, "endless_gravity.config.atmosphereGravityMax", Config.COMMON.atmosphereGravityMax,
                    0.0, 0.1, 0.08, TEXT_WHITE);
            addDouble(atmosphere, entry, "endless_gravity.config.atmosphereMuffleGain", Config.COMMON.atmosphereMuffleGain,
                    0.0, 1.0, 0.01, TEXT_WHITE);
            addDouble(atmosphere, entry, "endless_gravity.config.atmosphereMuffleGainHF", Config.COMMON.atmosphereMuffleGainHF,
                    0.0, 1.0, 0.005, TEXT_GRAY);
            cat.addEntry(atmosphere.build());
            allOverworld.add(atmosphere.build());

            SubCategoryBuilder layersSub = entry.startSubCategory(
                            colored("endless_gravity.config.atmosphereLayers", TEXT_BLUE))
                    .setExpanded(false)
                    .setTooltip(text("endless_gravity.config.atmosphereLayers.tooltip"));
            for (int i = 0; i < AtmosphereLayers.LAYER_COUNT; i++) {
                final int index = i;
                AtmosphereLayers.Layer layer = layers.get(index);
                SubCategoryBuilder layerSub = entry.startSubCategory(
                                colored("endless_gravity.config.layerShort", TEXT_GRAY, index + 1))
                        .setExpanded(true);
                layerSub.add(entry.startDoubleField(text("endless_gravity.config.columnAltitude"), layer.altitude())
                        .setMin(-64.0).setMax(4000.0)
                        .setDefaultValue(AtmosphereLayers.defaults().get(index).altitude())
                        .setSaveConsumer(value -> layers.set(index, new AtmosphereLayers.Layer(value, layers.get(index).pressure())))
                        .build());
                layerSub.add(entry.startDoubleField(colored("endless_gravity.config.columnPressure", TEXT_GRAY), layer.pressure())
                        .setMin(0.0).setMax(2.0)
                        .setDefaultValue(AtmosphereLayers.defaults().get(index).pressure())
                        .setSaveConsumer(value -> layers.set(index, new AtmosphereLayers.Layer(layers.get(index).altitude(), value)))
                        .build());
                layersSub.add(layerSub.build());
            }
            cat.addEntry(layersSub.build());
            allOverworld.add(layersSub.build());

            SubCategoryBuilder temperature = group(entry, "endless_gravity.config.group.temperature");
            addToggle(temperature, entry, "endless_gravity.config.enableTemperature", Config.COMMON.enableTemperature, true);
            addIntSlider(temperature, entry, "endless_gravity.config.temperatureFreezeInterval", Config.COMMON.temperatureFreezeInterval,
                    1, 200, 20, TEXT_GRAY);
            cat.addEntry(temperature.build());
            allOverworld.add(temperature.build());

            SubCategoryBuilder oxygen = group(entry, "endless_gravity.config.group.oxygen");
            addToggle(oxygen, entry, "endless_gravity.config.enableOxygen", Config.COMMON.enableOxygen, true);
            addIntSlider(oxygen, entry, "endless_gravity.config.oxygenRate", Config.COMMON.oxygenRate,
                    1, 100, 8, TEXT_GRAY);
            addIntSlider(oxygen, entry, "endless_gravity.config.oxygenTankCapacity", Config.COMMON.oxygenTankCapacity,
                    50, 5000, 1000, TEXT_WHITE);
            addIntSlider(oxygen, entry, "endless_gravity.config.oxygenRechargeRate", Config.COMMON.oxygenRechargeRate,
                    1, 200, 5, TEXT_GRAY);
            addIntSlider(oxygen, entry, "endless_gravity.config.oxygenSuffocationFadeTicks", Config.COMMON.oxygenSuffocationFadeTicks,
                    20, 600, 100, TEXT_GRAY);
            cat.addEntry(oxygen.build());
            allOverworld.add(oxygen.build());

            if (sableLoaded) {
                SubCategoryBuilder sable = group(entry, "endless_gravity.config.group.sable_overworld");
                sable.add(entry.startTextDescription(colored("endless_gravity.config.restartWarning", TEXT_AMBER)).build());
                addToggle(sable, entry, "endless_gravity.config.overworldSableGravity", Config.COMMON.overworldSableGravity, true);
                addIntSlider(sable, entry, "endless_gravity.config.overworldSableDatapackPriority", Config.COMMON.overworldSableDatapackPriority,
                        1, 9999, 2000, TEXT_GRAY);
                cat.addEntry(sable.build());
                allOverworld.add(sable.build());
            }

            all.addEntry(allOverworld.build());
        }

        private void buildGeneral(ConfigBuilder builder, ConfigEntryBuilder entry, ConfigCategory all) {
            ConfigCategory cat = category(builder, "endless_gravity.config.section.general");
            SubCategoryBuilder allGeneral = group(entry, "endless_gravity.config.section.general");
            var particle = entry.startDoubleField(colored("endless_gravity.config.particleGravityMultiplier", TEXT_WHITE),
                            Config.COMMON.particleGravityMultiplier.get())
                    .setMin(0.0).setMax(1.0).setDefaultValue(0.3)
                    .setSaveConsumer(Config.COMMON.particleGravityMultiplier::set)
                    .build();
            cat.addEntry(particle);
            allGeneral.add(particle);
            var mode = entry.startEnumSelector(text("endless_gravity.config.fallDamageMode"), FallDamageMode.class,
                            FallDamageMode.fromInt(Config.COMMON.fallDamageMode.get()))
                    .setDefaultValue(FallDamageMode.VELOCITY)
                    .setEnumNameProvider(e -> colored("endless_gravity.config.fallDamageMode."
                                    + ((FallDamageMode) e).name().toLowerCase(Locale.ROOT),
                            switch ((FallDamageMode) e) {
                                case DISABLED -> TEXT_RED;
                                case VELOCITY -> TEXT_GREEN;
                                default -> TEXT_WHITE;
                            }))
                    .setSaveConsumer(m -> Config.COMMON.fallDamageMode.set(m.ordinal()))
                    .build();
            cat.addEntry(mode);
            allGeneral.add(mode);
            var scale = entry.startDoubleField(colored("endless_gravity.config.fallDamageVelocityScale", TEXT_WHITE),
                            Config.COMMON.fallDamageVelocityScale.get())
                    .setMin(0.1).setMax(10.0).setDefaultValue(1.0)
                    .setSaveConsumer(Config.COMMON.fallDamageVelocityScale::set)
                    .build();
            cat.addEntry(scale);
            allGeneral.add(scale);
            var minVelocity = entry.startDoubleField(colored("endless_gravity.config.fallDamageMinVelocity", TEXT_GRAY),
                            Config.COMMON.fallDamageMinVelocity.get())
                    .setMin(0.0).setMax(5.0).setDefaultValue(0.6)
                    .setSaveConsumer(Config.COMMON.fallDamageMinVelocity::set)
                    .build();
            cat.addEntry(minVelocity);
            allGeneral.add(minVelocity);
            all.addEntry(allGeneral.build());
        }

        private void addToggle(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                               ModConfigSpec.BooleanValue value, boolean defaultValue) {
            group.add(entry.startBooleanToggle(text(key), value.get())
                    .setDefaultValue(defaultValue)
                    .setSaveConsumer(value::set)
                    .setYesNoTextSupplier(v -> colored("endless_gravity.config." + v, v ? TEXT_GREEN : TEXT_GRAY))
                    .build());
        }

        private void addDouble(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                               ModConfigSpec.DoubleValue value, double min, double max, double defaultValue, int color) {
            group.add(entry.startDoubleField(colored(key, color), value.get())
                    .setMin(min).setMax(max).setDefaultValue(defaultValue)
                    .setSaveConsumer(value::set)
                    .build());
        }

        private void addIntSlider(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                                  ModConfigSpec.IntValue value, int min, int max, int defaultValue, int color) {
            group.add(entry.startIntSlider(colored(key, color), value.get(), min, max)
                    .setDefaultValue(defaultValue)
                    .setSaveConsumer(value::set)
                    .build());
        }

        private void loadLayers() {
            layers.clear();
            List<AtmosphereLayers.Layer> parsed = AtmosphereLayers.parse(Config.COMMON.atmosphereLayers.get());
            for (int i = 0; i < AtmosphereLayers.LAYER_COUNT; i++) {
                layers.add(i < parsed.size() ? parsed.get(i) : AtmosphereLayers.defaults().get(i));
            }
        }

        private void finishSave() {
            Config.COMMON.atmosphereLayers.set(AtmosphereLayers.serialize(layers));
            if (sableLoaded) {
                SableDatapackHandler.generateDatapack();
            }
        }

        private static SubCategoryBuilder group(ConfigEntryBuilder entry, String key) {
            return entry.startSubCategory(colored(key, TEXT_BLUE)).setExpanded(true);
        }

        private static ConfigCategory category(ConfigBuilder builder, String key) {
            return builder.getOrCreateCategory(text(key));
        }

        private static Component text(String key, Object... args) {
            return Component.translatable(key, args);
        }

        private static Component colored(String key, int color, Object... args) {
            return Component.translatable(key, args).withColor(color);
        }
    }
}
