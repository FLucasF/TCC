package com.loja.pagamento;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return new BigDecimal("3.49").setScale(2);
    }

    @Override
    public boolean validarParcelamento(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido, int parcelas) {
        return parcelas == 1 && totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
    }
}
