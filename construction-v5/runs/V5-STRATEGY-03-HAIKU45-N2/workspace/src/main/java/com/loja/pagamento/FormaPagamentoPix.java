package com.loja.pagamento;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class FormaPagamentoPix implements FormaPagamento {
    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        BigDecimal desconto = total.multiply(BigDecimal.valueOf(0.05));
        desconto = Arredondamento.arredondarParaPar(desconto);
        return desconto.negate();
    }

    @Override
    public boolean podeParcelar() {
        return false;
    }

    @Override
    public Integer getMaximoParcelas() {
        return 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal total) {
        return true;
    }
}
