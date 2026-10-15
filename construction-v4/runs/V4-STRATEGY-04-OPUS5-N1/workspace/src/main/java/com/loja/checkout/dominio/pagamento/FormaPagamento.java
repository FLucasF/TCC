package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** Uma forma de pagamento: seu parcelamento, suas limitacoes e seu ajuste no total. */
public interface FormaPagamento {

    String codigo();

    /** Se a forma de pagamento aceita esse numero de parcelas. */
    boolean parcelamentoPermitido(int parcelas);

    /** Se a forma de pagamento atende um pedido desse valor. */
    boolean atende(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
