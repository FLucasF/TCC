package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Codificado;
import java.math.BigDecimal;

/** Uma forma de pagamento: parcelas aceitas, pedidos que atende e ajuste sobre o total. */
public interface FormaPagamento extends Codificado {

    boolean parcelasPermitidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    Pagamento pagar(BigDecimal totalPedido, int parcelas);
}
