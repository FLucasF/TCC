package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Forma que nao parcela: uma parcela unica igual ao valor final. */
abstract class AVista implements FormaPagamento {

    @Override
    public final boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public final Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorFinal = valorFinal(totalPedido);
        return new Cobranca(valorFinal, valorFinal);
    }

    abstract BigDecimal valorFinal(BigDecimal totalPedido);
}
