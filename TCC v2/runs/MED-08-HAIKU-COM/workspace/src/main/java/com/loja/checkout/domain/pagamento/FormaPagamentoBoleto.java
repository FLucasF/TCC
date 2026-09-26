package com.loja.checkout.domain.pagamento;

import com.loja.checkout.utils.Arredondamento;
import java.math.BigDecimal;

public class FormaPagamentoBoleto implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("1000.00");

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return Arredondamento.arredondarParaCentavos(totalFinal);
    }

    @Override
    public boolean podeAplicar(BigDecimal total, int parcelas) {
        return parcelas == 1 && total.compareTo(LIMITE_MAXIMO) <= 0;
    }

    @Override
    public String obterCodigo() {
        return "BOLETO";
    }
}
