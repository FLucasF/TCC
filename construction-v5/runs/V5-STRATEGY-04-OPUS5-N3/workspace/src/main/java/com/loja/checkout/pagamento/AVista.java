package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Forma sempre à vista: uma parcela só, igual ao valor final. */
abstract class AVista implements FormaPagamento {

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalFinalAVista(totalPedido);
        return new Cobranca(totalFinal, totalFinal);
    }

    protected abstract BigDecimal totalFinalAVista(BigDecimal totalPedido);
}
