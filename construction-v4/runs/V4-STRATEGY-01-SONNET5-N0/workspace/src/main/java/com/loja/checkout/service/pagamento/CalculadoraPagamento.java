package com.loja.checkout.service.pagamento;

import com.loja.checkout.enums.FormaPagamento;

import java.math.BigDecimal;

public interface CalculadoraPagamento {

    FormaPagamento getForma();

    boolean parcelasValidas(int parcelas);

    boolean disponivelPara(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
