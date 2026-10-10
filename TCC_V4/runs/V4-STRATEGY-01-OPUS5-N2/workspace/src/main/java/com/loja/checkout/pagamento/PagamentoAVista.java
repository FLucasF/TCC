package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Forma de pagamento sem parcelamento: so' 1 vez, e a parcela e' o valor final. */
public interface PagamentoAVista extends FormaPagamento {

    /** O valor final a' vista. */
    BigDecimal totalFinal(BigDecimal totalPedido);

    @Override
    default Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalFinal(totalPedido);
        return new Pagamento(totalFinal, totalFinal);
    }

    @Override
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }
}
