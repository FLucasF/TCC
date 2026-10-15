package com.loja.checkout.pagamento;

/** Pix e boleto são sempre à vista. */
abstract class PagamentoAVista implements FormaPagamento {

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }
}
