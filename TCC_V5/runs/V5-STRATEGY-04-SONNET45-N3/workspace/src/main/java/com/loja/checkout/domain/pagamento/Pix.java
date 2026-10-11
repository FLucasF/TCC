package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Arredondamento;
import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public boolean aceitaParcelas(int numeroParcelas) {
        return numeroParcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int numeroParcelas) {
        return calcularDesconto(totalPedido).negate();
    }

    @Override
    public BigDecimal calcularValorFinal(BigDecimal totalPedido, int numeroParcelas) {
        return totalPedido.subtract(calcularDesconto(totalPedido));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int numeroParcelas) {
        return calcularValorFinal(totalPedido, numeroParcelas);
    }

    private BigDecimal calcularDesconto(BigDecimal totalPedido) {
        BigDecimal desconto = totalPedido.multiply(PERCENTUAL_DESCONTO);
        return Arredondamento.arredondar(desconto);
    }
}
