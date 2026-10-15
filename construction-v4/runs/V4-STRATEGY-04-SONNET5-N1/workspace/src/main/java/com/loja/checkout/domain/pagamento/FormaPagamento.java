package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Codificavel;

import java.math.BigDecimal;

public interface FormaPagamento extends Codificavel {

    boolean parcelasPermitidas(int parcelas);

    boolean disponivelPara(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
