package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;

/** Até 3x sem juros; de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
public final class Cartao implements FormaPagamento {

    private static final int MAXIMO_SEM_JUROS = 3;
    private static final int MAXIMO_PARCELAS = 12;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    private Cobranca semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
        return new Cobranca(totalPedido, parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private Cobranca comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal um = BigDecimal.ONE;
        BigDecimal fatorFinal = um.divide(um.add(TAXA_MENSAL).pow(parcelas, PRECISAO), PRECISAO);
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(um.subtract(fatorFinal), PRECISAO));
        return new Cobranca(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
    }
}
