package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;

/** Cartão de crédito: até 3x sem juros, de 4x a 12x com juros pela tabela Price. */
class Cartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return new Cobranca(Dinheiro.centavos(totalPedido), divide(totalPedido, parcelas));
        }
        BigDecimal parcela = parcelaPrice(totalPedido, parcelas);
        return new Cobranca(Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
    }

    private BigDecimal divide(BigDecimal total, int parcelas) {
        return Dinheiro.centavos(total.divide(BigDecimal.valueOf(parcelas), PRECISAO));
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        return Dinheiro.centavos(total.multiply(TAXA_MENSAL).divide(divisor, PRECISAO));
    }
}
