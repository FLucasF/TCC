package com.loja.checkout.estrategia.pagamento;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class PagamentoPix implements EstrategiaPagamento {
    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
        desconto = Arredondamento.arredondarMeioParaPar(desconto);
        return desconto.negate();
    }

    @Override
    public void validar(int parcelas, BigDecimal totalPedido) throws IllegalArgumentException {
        if (parcelas != 1) {
            throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
        }
    }
}
