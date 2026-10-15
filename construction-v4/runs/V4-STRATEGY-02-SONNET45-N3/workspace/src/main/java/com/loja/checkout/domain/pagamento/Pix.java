package com.loja.checkout.domain.pagamento;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Pix implements FormaPagamento {

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.percentual(totalPedido, 5.0);
        return desconto.negate();
    }

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceitaTotal(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal totalFinal, int parcelas) {
        return totalFinal;
    }
}
