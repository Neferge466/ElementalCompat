package com.elementalcompat;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue BASE_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General settings").push("general");
        BASE_MULTIPLIER = builder
                .comment("Base damage multiplier")
                .defineInRange("baseMultiplier", 1.0, 0.0, 10.0);
        builder.pop();

        SPEC = builder.build();
    }
}
