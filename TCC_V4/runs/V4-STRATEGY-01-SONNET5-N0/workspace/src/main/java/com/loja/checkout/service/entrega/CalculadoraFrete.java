package com.loja.checkout.service.entrega;

import com.loja.checkout.enums.ModalidadeEntrega;

import java.math.BigDecimal;

public interface CalculadoraFrete {

    ModalidadeEntrega getModalidade();

    boolean disponivelPara(BigDecimal pesoTotalKg);

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoDias();
}
