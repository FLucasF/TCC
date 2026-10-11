package com.loja.checkout.service.pagamento;

import java.math.BigDecimal;

public interface FormaPagamentoCalculo {

    String getCodigo();

    boolean parcelasValidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
