package com.loja.checkout.clube;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Ouro implements NivelClube {

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BeneficiosClube calcular(BigDecimal subtotalProdutos) {
        BigDecimal credito = Arredondamento.centavos(
                subtotalProdutos.multiply(new BigDecimal("0.05"))
        );
        boolean brinde = subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        return new BeneficiosClube(credito, true, brinde);
    }
}
