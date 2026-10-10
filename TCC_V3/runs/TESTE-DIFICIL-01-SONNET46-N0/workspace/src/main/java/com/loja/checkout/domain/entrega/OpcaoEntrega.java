package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface OpcaoEntrega {

    String getCodigo();

    BigDecimal calcularFrete(BigDecimal pesoKgTotal);

    int getPrazoDias();

    boolean isDisponivel(BigDecimal pesoKgTotal);

    boolean temSeguro();
}
