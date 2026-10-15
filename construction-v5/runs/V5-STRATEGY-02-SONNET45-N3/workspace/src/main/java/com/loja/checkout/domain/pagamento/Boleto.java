package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("1000.00");

    @Override
    public AjustePagamento calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal totalFinal = total.add(TARIFA);

        return new AjustePagamento(TARIFA, totalFinal, totalFinal);
    }

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal total) {
        return total.compareTo(LIMITE_MAXIMO) <= 0;
    }
}
