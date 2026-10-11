package com.loja.checkout.estrategia.pagamento;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Moeda.arredondar(totalPedido.multiply(PERCENTUAL_DESCONTO));
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(calcularAjuste(totalPedido, parcelas));
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas == 1;
    }
}
