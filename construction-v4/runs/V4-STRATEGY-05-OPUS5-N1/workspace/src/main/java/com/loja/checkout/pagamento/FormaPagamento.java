package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/** Uma forma de pagar o pedido. Uma implementacao por forma. */
public interface FormaPagamento extends Identificavel {

    /** Se este numero de parcelas e permitido nesta forma de pagamento. */
    boolean permiteParcelas(int parcelas);

    /** Se a forma de pagamento atende um pedido deste total. */
    boolean atende(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
