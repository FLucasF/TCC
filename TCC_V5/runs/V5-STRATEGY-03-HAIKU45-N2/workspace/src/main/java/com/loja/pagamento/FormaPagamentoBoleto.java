package com.loja.pagamento;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class FormaPagamentoBoleto implements FormaPagamento {
    private static final BigDecimal TAXA = BigDecimal.valueOf(3.49);
    private static final BigDecimal LIMITE_MAXIMO = BigDecimal.valueOf(1000.00);

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        return Arredondamento.arredondarParaPar(TAXA);
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
        return total.compareTo(LIMITE_MAXIMO) <= 0;
    }
}
