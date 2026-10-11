package com.loja.checkout.clube;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BeneficiosClube calcular(BigDecimal subtotalProdutos) {
        BigDecimal credito = Arredondamento.centavos(
                subtotalProdutos.multiply(new BigDecimal("0.02"))
        );
        return new BeneficiosClube(credito, false, false);
    }
}
