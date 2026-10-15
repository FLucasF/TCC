package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("1000.00");

    @Override
    public boolean aceitaParcelas(int numParcelas) {
        return numParcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);

        return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
    }
}
