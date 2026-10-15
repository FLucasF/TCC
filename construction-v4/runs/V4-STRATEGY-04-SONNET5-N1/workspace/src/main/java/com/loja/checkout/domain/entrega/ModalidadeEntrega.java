package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.Codificavel;

import java.math.BigDecimal;

public interface ModalidadeEntrega extends Codificavel {

    boolean disponivelPara(BigDecimal pesoTotalKg);

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoDias();
}
