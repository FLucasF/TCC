package com.loja.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularAjuste(BigDecimal total, Integer parcelas);
    boolean podeParcelar();
    Integer getMaximoParcelas();
    boolean estaDisponivel(BigDecimal total);
}
