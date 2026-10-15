package com.loja.resumo.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    boolean parcelasValidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    AjustePagamento calcular(BigDecimal totalPedido, int parcelas);
}
