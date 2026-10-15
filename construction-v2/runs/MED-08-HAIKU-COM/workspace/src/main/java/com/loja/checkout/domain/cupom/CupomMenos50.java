package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import java.util.List;

public class CupomMenos50 implements Cupom {
    private static final BigDecimal DESCONTO_FIXO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemCupom> itens) {
        return DESCONTO_FIXO;
    }

    @Override
    public boolean podeAplicar(BigDecimal subtotal, List<ItemCupom> itens) {
        return subtotal.compareTo(MINIMO) >= 0;
    }

    @Override
    public String obterCodigo() {
        return "MENOS50";
    }
}
