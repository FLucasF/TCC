package com.loja.checkout.clube;

import java.math.BigDecimal;
import com.loja.checkout.util.Arredonda;

public class ClubePrata implements BeneficiosClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Arredonda.arredonda(subtotalProdutos.multiply(new BigDecimal("0.02")));
    }

    @Override
    public boolean temFretGratis() {
        return false;
    }

    @Override
    public boolean temBrindeAcima500() {
        return false;
    }
}
