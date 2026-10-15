package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String getCodigo();

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    boolean isDisponivel(BigDecimal totalPedido);

    boolean aceitaParcelas(int parcelas);
}
