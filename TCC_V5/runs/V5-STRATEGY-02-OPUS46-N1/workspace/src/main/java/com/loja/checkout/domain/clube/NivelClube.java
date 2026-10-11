package com.loja.checkout.domain.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, null),
    PRATA(new BigDecimal("0.02"), false, null),
    OURO(new BigDecimal("0.05"), true, new BigDecimal("500"));

    private static final Map<String, NivelClube> POR_CODIGO = Map.of(
            "BRONZE", BRONZE,
            "PRATA", PRATA,
            "OURO", OURO
    );

    private final BigDecimal taxaCashback;
    private final boolean freteGratis;
    private final BigDecimal limiarBrinde;

    NivelClube(BigDecimal taxaCashback, boolean freteGratis, BigDecimal limiarBrinde) {
        this.taxaCashback = taxaCashback;
        this.freteGratis = freteGratis;
        this.limiarBrinde = limiarBrinde;
    }

    public boolean temFreteGratis() {
        return freteGratis;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return limiarBrinde != null && subtotalProdutos.compareTo(limiarBrinde) > 0;
    }

    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(taxaCashback).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Optional<NivelClube> buscar(String codigo) {
        return Optional.ofNullable(POR_CODIGO.get(codigo));
    }
}
