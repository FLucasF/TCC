package com.loja.checkout.pagamento;

import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;

public class BoletoStrategy implements PagamentoStrategy {
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("1000.00");
    private static final BigDecimal TARIFA = new BigDecimal("3.49");

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        return Arredondamento.arredondar(totalPedido.add(TARIFA));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return calcularTotalFinal(totalPedido, parcelas);
    }
}
