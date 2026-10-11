package com.loja.checkout.clube;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.NivelClube;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class OuroBeneficioClube implements BeneficioClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_PRODUTOS_BRINDE = new BigDecimal("500.00");

    @Override
    public NivelClube nivel() {
        return NivelClube.OURO;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_PRODUTOS_BRINDE) > 0;
    }
}
