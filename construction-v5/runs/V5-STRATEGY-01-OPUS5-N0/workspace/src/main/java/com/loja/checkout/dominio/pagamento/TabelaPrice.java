package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/**
 * Calculo de parcela com juros compostos, o mesmo do crediario:
 * parcela = total x taxa / (1 - (1 + taxa)^-parcelas).
 */
public final class TabelaPrice {

    private static final BigDecimal UM = BigDecimal.ONE;

    private TabelaPrice() {
    }

    /** Valor da parcela, arredondado para centavos. */
    public static BigDecimal parcela(BigDecimal total, BigDecimal taxaMensal, int parcelas) {
        BigDecimal fator = UM.add(taxaMensal).pow(parcelas, Dinheiro.PRECISAO);
        BigDecimal divisor = UM.subtract(UM.divide(fator, Dinheiro.PRECISAO));
        return Dinheiro.centavos(total.multiply(taxaMensal).divide(divisor, Dinheiro.PRECISAO));
    }
}
