package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    boolean parcelasPermitidas(int parcelas);

    boolean disponivelPara(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
