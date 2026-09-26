package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    boolean parcelasValidas(int parcelas);

    boolean disponivelPara(BigDecimal totalPedido, int parcelas);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
