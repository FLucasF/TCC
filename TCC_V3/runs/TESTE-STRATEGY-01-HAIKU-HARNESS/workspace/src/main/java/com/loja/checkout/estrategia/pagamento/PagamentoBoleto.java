package com.loja.checkout.estrategia.pagamento;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class PagamentoBoleto implements EstrategiaPagamento {
    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal tarifa = new BigDecimal("3.49");
        return Arredondamento.arredondarMeioParaPar(tarifa);
    }

    @Override
    public void validar(int parcelas, BigDecimal totalPedido) throws IllegalArgumentException {
        if (parcelas != 1) {
            throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
        }
        if (totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }
}
