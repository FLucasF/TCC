package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String getCodigo();

    boolean isParcelamentoValido(int parcelas);

    boolean isDisponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
