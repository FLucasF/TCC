package com.loja.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    boolean validarParcelamento(int parcelas);
    boolean estaDisponivel(BigDecimal totalPedido, int parcelas);
}
