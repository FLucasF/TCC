package com.loja.dominio;

import java.math.BigDecimal;

public enum Regiao {
    SUDESTE("0.01", null, 0),
    SUL("0.01", null, 0),
    CENTRO_OESTE("0.015", null, 0),
    NORTE("0.025", "30.00", 3),
    NORDESTE("0.02", "30.00", 2);

    private final String taxaSeguro;
    private final BigDecimal limiteSeguro;
    private final int diasExtraDespacho;

    Regiao(String taxaSeguro, String limiteSeguro, int diasExtraDespacho) {
        this.taxaSeguro = taxaSeguro;
        this.limiteSeguro = limiteSeguro == null ? null : Dinheiro.de(limiteSeguro);
        this.diasExtraDespacho = diasExtraDespacho;
    }

    public int diasExtraDespacho() {
        return diasExtraDespacho;
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        BigDecimal valor = Dinheiro.porcentagem(subtotalProdutos, taxaSeguro);
        if (limiteSeguro != null && valor.compareTo(limiteSeguro) > 0) {
            return limiteSeguro;
        }
        return valor;
    }

    public static Regiao de(String texto) {
        if (texto == null) {
            return null;
        }
        for (Regiao r : values()) {
            if (r.name().equals(texto)) {
                return r;
            }
        }
        return null;
    }
}
