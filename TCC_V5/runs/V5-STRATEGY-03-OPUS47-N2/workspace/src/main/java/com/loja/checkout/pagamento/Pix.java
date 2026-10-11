package com.loja.checkout.pagamento;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.ErroPedido;

import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) throw new ErroPedido("PARCELAMENTO_INVALIDO");
    }

    @Override
    public void validar(BigDecimal totalPedido) {}

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredonda(totalPedido.multiply(DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
    }
}
