package com.loja.checkout.pagamento;

/** Base das formas que são sempre à vista. */
abstract class PagamentoAVista implements FormaPagamento {

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }
}
