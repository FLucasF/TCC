package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.FormaPagamento;
import java.math.BigDecimal;

/**
 * Cada forma de pagamento tem sua propria regra de parcelamento, de
 * disponibilidade e de ajuste sobre o total do pedido (desconto, tarifa ou
 * juros). Escolher entre elas e uma busca no registro, nao uma condicao.
 */
public interface Pagamento {

    FormaPagamento forma();

    boolean parcelasValidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
