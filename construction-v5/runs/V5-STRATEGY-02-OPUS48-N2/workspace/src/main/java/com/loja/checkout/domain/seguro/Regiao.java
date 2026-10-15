package com.loja.checkout.domain.seguro;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.erro.CheckoutException;
import com.loja.checkout.domain.erro.ErroCheckout;

import java.math.BigDecimal;

/**
 * Regiao do cliente. Para o seguro, so a porcentagem muda de uma regiao para
 * outra; a conta e a mesma em todas, por isso ela vive aqui e nao em classes
 * separadas por regiao.
 */
public enum Regiao {
    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final BigDecimal percentual;

    Regiao(String percentual) {
        this.percentual = new BigDecimal(percentual);
    }

    /** Seguro: a porcentagem da regiao sobre o valor dos produtos, arredondado. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentual);
    }

    public static Regiao resolver(String codigo) {
        if (codigo != null) {
            for (Regiao regiao : values()) {
                if (regiao.name().equals(codigo)) {
                    return regiao;
                }
            }
        }
        throw new CheckoutException(ErroCheckout.REGIAO_INVALIDA);
    }
}
