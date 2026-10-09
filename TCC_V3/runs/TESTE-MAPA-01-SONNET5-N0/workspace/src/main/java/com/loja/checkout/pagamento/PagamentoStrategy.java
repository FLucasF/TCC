package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface PagamentoStrategy {

    String codigo();

    boolean parcelasValidas(int parcelas);

    boolean disponivelPara(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
