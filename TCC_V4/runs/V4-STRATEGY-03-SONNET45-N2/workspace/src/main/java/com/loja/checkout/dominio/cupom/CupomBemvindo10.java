package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.AplicadorCupom;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CupomBemvindo10 implements AplicadorCupom {

    @Override
    public boolean podeAplicar(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return subtotalProdutos.multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
