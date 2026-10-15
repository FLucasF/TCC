package com.loja.checkout.clube;

import java.math.BigDecimal;
import com.loja.checkout.util.Arredonda;

public class ClubeOuro implements BeneficiosClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Arredonda.arredonda(subtotalProdutos.multiply(new BigDecimal("0.05")));
    }

    @Override
    public boolean temFretGratis() {
        return true;
    }

    @Override
    public boolean temBrindeAcima500() {
        return true;
    }
}
