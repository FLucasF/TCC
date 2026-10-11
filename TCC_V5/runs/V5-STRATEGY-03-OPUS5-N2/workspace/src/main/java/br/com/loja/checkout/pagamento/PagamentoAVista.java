package br.com.loja.checkout.pagamento;

/** Forma de pagamento que nao parcela: e sempre a vista. */
public interface PagamentoAVista extends FormaPagamento {

    @Override
    default boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }
}
