package com.loja.strategy;

import com.loja.model.ClubTier;

public class ClubFactory {
    public static ClubBenefitsStrategy create(ClubTier tier) {
        if (tier == null) return new BronzeClub();

        return switch (tier) {
            case BRONZE -> new BronzeClub();
            case SILVER -> new SilverClub();
            case GOLD -> new GoldClub();
        };
    }
}
