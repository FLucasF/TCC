package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Parte comum das formas que nao parcelam: sempre 1 parcela igual ao total final. */
abstract class AVista implements FormaPagamento {

    private static final int PARCELA_UNICA = 1;

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == PARCELA_UNICA;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalFinal(totalPedido);
        return new Cobranca(totalFinal, PARCELA_UNICA, totalFinal);
    }

    protected abstract BigDecimal totalFinal(BigDecimal totalPedido);
}
