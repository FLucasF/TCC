package com.loja.pedidos.dominio;

import java.math.BigDecimal;

public enum Regiao {
    SUDESTE("0.01", null, 0),
    SUL("0.01", null, 0),
    CENTRO_OESTE("0.015", null, 0),
    NORTE("0.025", "30.00", 3),
    NORDESTE("0.02", "30.00", 2);

    private final BigDecimal percentualSeguro;
    private final BigDecimal limiteSeguro;
    private final int diasExtraDespacho;

    Regiao(String percentualSeguro, String limiteSeguro, int diasExtraDespacho) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
        this.limiteSeguro = limiteSeguro == null ? null : Dinheiro.valor(limiteSeguro);
        this.diasExtraDespacho = diasExtraDespacho;
    }

    public BigDecimal seguro(BigDecimal valorProdutos) {
        BigDecimal seguro = Dinheiro.fracao(valorProdutos, percentualSeguro);
        return limiteSeguro == null ? seguro : Dinheiro.minimo(seguro, limiteSeguro);
    }

    public int diasExtraDespacho() {
        return diasExtraDespacho;
    }

    public static Regiao de(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Regiao r : values()) {
            if (r.name().equals(codigo)) {
                return r;
            }
        }
        return null;
    }
}
