package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Regiao do cliente. O seguro muda so na porcentagem cobrada pela seguradora,
 * por isso a conta fica num lugar so.
 */
public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal aliquotaSeguro;

    Regiao(String aliquotaSeguro) {
        this.aliquotaSeguro = new BigDecimal(aliquotaSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquotaSeguro);
    }

    public static Regiao exigir(String codigo) {
        for (Regiao regiao : values()) {
            if (regiao.name().equals(codigo)) {
                return regiao;
            }
        }
        throw new PedidoRecusadoException(MotivoRecusa.REGIAO_INVALIDA);
    }
}
