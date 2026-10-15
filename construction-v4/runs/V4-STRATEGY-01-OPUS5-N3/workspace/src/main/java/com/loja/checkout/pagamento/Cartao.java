package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Cartao de credito: ate 3x sem juros, de 4x a 12x com juros (tabela Price). */
public final class Cartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Liquidacao liquidar(BigDecimal totalPedido, int parcelas) {
        return parcelas <= PARCELAS_SEM_JUROS
                ? semJuros(totalPedido, parcelas)
                : comJuros(totalPedido, parcelas);
    }

    /** O valor final e o proprio total do pedido; a parcela e o total dividido. */
    private Liquidacao semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = totalPedido.divide(
                BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        return new Liquidacao(totalPedido, valorParcela);
    }

    /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
    private Liquidacao comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, PRECISAO));
        BigDecimal totalFinal = Dinheiro.centavos(
                valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Liquidacao(totalFinal, valorParcela);
    }
}
