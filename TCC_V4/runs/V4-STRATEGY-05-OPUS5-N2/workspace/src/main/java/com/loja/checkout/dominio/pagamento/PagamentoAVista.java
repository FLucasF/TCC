package com.loja.checkout.dominio.pagamento;

/** Forma de pagamento que nao parcela: e sempre a vista. */
public interface PagamentoAVista extends FormaPagamento {

    int PARCELA_UNICA = 1;

    @Override
    default boolean parcelasPermitidas(int parcelas) {
        return parcelas == PARCELA_UNICA;
    }
}
