package com.loja.checkout.cupom;

import java.math.BigDecimal;

public interface CupomHandler {

    String getCodigo();

    boolean aplicavel(DadosPedido pedido);

    BigDecimal calcularDesconto(DadosPedido pedido);
}
