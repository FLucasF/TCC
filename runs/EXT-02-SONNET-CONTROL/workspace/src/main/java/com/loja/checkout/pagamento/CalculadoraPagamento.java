package com.loja.checkout.pagamento;

import com.loja.checkout.domain.FormaPagamento;

import java.math.BigDecimal;

public interface CalculadoraPagamento {

    FormaPagamento formaPagamento();

    boolean parcelasValidas(int parcelas);

    boolean disponivelPara(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
