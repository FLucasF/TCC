package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    AjustePagamento calcularAjuste(BigDecimal total, int parcelas);
    boolean validarParcelas(int parcelas);
    boolean estaDisponivel(BigDecimal total);
}
