package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** parcela = total x taxa / (1 - (1 + taxa) ^ -parcelas) */
final class TabelaPrice {

    private TabelaPrice() {
    }

    static BigDecimal parcela(BigDecimal total, BigDecimal taxaMensal, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(taxaMensal).pow(parcelas, Dinheiro.PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.PRECISAO));
        return Dinheiro.emCentavos(total.multiply(taxaMensal).divide(divisor, Dinheiro.PRECISAO));
    }
}
