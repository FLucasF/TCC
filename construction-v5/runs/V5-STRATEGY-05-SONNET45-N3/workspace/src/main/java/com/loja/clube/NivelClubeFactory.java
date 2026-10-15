package com.loja.clube;

public class NivelClubeFactory {

    public static NivelClube criar(String codigo) {
        return switch (codigo) {
            case "BRONZE" -> new Bronze();
            case "PRATA" -> new Prata();
            case "OURO" -> new Ouro();
            default -> null;
        };
    }
}
