package com.loja.checkout.domain.pagamento;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return Dinheiro.de(3.49);
    }

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceitaTotal(BigDecimal totalPedido) {
        return totalPedido.compareTo(Dinheiro.de(1000.00)) <= 0;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal totalFinal, int parcelas) {
        return totalFinal;
    }
}
