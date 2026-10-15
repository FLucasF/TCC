package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valores em dinheiro: sempre com 2 casas decimais e nunca negativos. */
public final class Dinheiro {

    public static final BigDecimal ZERO = valor(BigDecimal.ZERO);

    private Dinheiro() {
    }

    /**
     * Normaliza para 2 casas decimais. As contas do servico sao so somas e
     * subtracoes, entao nao ha o que arredondar; o modo de arredondamento
     * existe apenas para o caso de o site mandar um valor com mais casas.
     */
    public static BigDecimal valor(BigDecimal bruto) {
        return bruto.setScale(2, RoundingMode.HALF_UP);
    }

    /** Subtrai sem deixar o resultado ficar negativo. */
    public static BigDecimal subtraiAteZero(BigDecimal valor, BigDecimal desconto) {
        BigDecimal resultado = valor.subtract(desconto);
        return valor(resultado.signum() < 0 ? BigDecimal.ZERO : resultado);
    }
}
