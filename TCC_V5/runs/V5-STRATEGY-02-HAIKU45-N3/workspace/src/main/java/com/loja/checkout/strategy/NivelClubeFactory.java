package com.loja.checkout.strategy;

public class NivelClubeFactory {

    public static NivelClube criar(String nivel) {
        return switch (nivel) {
            case "BRONZE" -> new NivelClubeBronze();
            case "PRATA" -> new NivelClubePrata();
            case "OURO" -> new NivelClubeOuro();
            default -> null;
        };
    }

}
