package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface CalculoPagamento {

    String getCodigo();

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    boolean isParcelamentoValido(int parcelas);

    boolean isDisponivel(BigDecimal totalPedido);
}
