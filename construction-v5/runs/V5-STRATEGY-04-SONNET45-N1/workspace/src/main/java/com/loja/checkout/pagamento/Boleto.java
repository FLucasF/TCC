package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public class Boleto implements CalculadoraPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("1000.00");

    @Override
    public boolean podeAtender(BigDecimal totalPedido) {
        return totalPedido.compareTo(VALOR_MAXIMO) <= 0;
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(TARIFA);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return calcularTotalFinal(totalPedido, parcelas);
    }
}
