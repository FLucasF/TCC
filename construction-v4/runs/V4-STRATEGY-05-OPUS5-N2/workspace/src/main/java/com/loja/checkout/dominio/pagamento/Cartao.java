package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/**
 * Cartao de credito: de 1x a 12x. Ate 3x sem juros; de 4x a 12x com juros de
 * 1,99% ao mes, pela tabela Price.
 */
public final class Cartao implements FormaPagamento {

    private static final int MINIMO_PARCELAS = 1;
    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        return parcelas <= MAXIMO_SEM_JUROS
                ? semJuros(totalPedido, parcelas)
                : comJuros(totalPedido, parcelas);
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= MINIMO_PARCELAS && parcelas <= MAXIMO_PARCELAS;
    }

    /** O valor final e o proprio total do pedido, dividido pelas parcelas. */
    private Pagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = Dinheiro.emCentavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO));
        return new Pagamento(Dinheiro.emCentavos(totalPedido), parcela);
    }

    /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
    private Pagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(fator, Dinheiro.PRECISAO));
        BigDecimal parcela = Dinheiro.emCentavos(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.PRECISAO));
        BigDecimal totalFinal = Dinheiro.emCentavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Pagamento(totalFinal, parcela);
    }
}
