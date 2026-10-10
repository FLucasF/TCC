package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.AplicadorCupom;
import java.math.BigDecimal;

public class CupomMenos50 implements AplicadorCupom {

    @Override
    public boolean podeAplicar(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return new BigDecimal("50.00");
    }
}
