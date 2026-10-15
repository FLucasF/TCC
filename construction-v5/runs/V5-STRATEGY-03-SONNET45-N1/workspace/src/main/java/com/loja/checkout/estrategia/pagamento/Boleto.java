package com.loja.checkout.estrategia.pagamento;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {
    private static final BigDecimal TARIFA = Moeda.de(3.49);
    private static final BigDecimal LIMITE = Moeda.de(1000.00);

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(calcularAjuste(totalPedido, parcelas));
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas == 1;
    }
}
