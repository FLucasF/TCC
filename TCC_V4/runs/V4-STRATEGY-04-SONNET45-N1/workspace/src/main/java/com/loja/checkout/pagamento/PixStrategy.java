package com.loja.checkout.pagamento;

import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;

public class PixStrategy implements PagamentoStrategy {
    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"));
        return Arredondamento.arredondar(desconto).negate();
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        BigDecimal ajuste = calcularAjuste(totalPedido, parcelas);
        return Arredondamento.arredondar(totalPedido.add(ajuste));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return calcularTotalFinal(totalPedido, parcelas);
    }
}
