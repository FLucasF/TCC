package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** A vista, com a tarifa que o banco cobra. Nao e aceito acima de R$ 1.000,00. */
public final class Boleto implements PagamentoAVista {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.emCentavos(totalPedido.add(TARIFA));
        return new Pagamento(totalFinal, totalFinal);
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }
}
