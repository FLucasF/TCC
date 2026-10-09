package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Codificavel;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento, com seu ajuste sobre o total do pedido, seu
 * parcelamento permitido e suas limitacoes.
 */
public interface FormaPagamento extends Codificavel {

    boolean aceitaParcelas(int parcelas);

    /** Se a forma atende este pedido (ex.: boleto so ate R$ 1.000,00). */
    boolean atende(BigDecimal totalPedido);

    Pagamento calcular(BigDecimal totalPedido, int parcelas);
}
