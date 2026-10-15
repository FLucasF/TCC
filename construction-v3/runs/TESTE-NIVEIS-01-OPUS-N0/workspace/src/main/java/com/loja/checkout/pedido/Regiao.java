package com.loja.checkout.pedido;

import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.CodigoErro;
import java.math.BigDecimal;

/**
 * Regiao do cliente. A conta do seguro e a mesma em todas as regioes
 * (percentual sobre o valor dos produtos); so muda o percentual.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }

    public static Regiao de(String codigo) {
        for (Regiao regiao : values()) {
            if (regiao.name().equals(codigo)) {
                return regiao;
            }
        }
        throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
    }
}
