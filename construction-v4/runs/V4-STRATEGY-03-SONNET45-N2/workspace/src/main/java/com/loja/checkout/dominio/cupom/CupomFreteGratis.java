package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.AplicadorCupom;
import java.math.BigDecimal;

public class CupomFreteGratis implements AplicadorCupom {

    @Override
    public boolean podeAplicar(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete;
    }
}
