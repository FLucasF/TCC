package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Arredondamento;
import java.math.BigDecimal;

public class Boleto implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("1000.00");

    @Override
    public boolean aceitaParcelas(int numeroParcelas) {
        return numeroParcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int numeroParcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularValorFinal(BigDecimal totalPedido, int numeroParcelas) {
        BigDecimal valorFinal = totalPedido.add(TARIFA);
        return Arredondamento.arredondar(valorFinal);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int numeroParcelas) {
        return calcularValorFinal(totalPedido, numeroParcelas);
    }
}
