package com.loja.checkout.service.clube;

import com.loja.checkout.service.Arredondamento;
import org.springframework.stereotype.Component;

@Component
public class ClubeOuro implements BeneficioClube {
    @Override
    public String getNivel() { return "OURO"; }

    @Override
    public boolean isFreteGratis() { return true; }

    @Override
    public double calcularCredito(double subtotalProdutos) {
        return Arredondamento.arredondar(subtotalProdutos * 0.05);
    }

    @Override
    public boolean temBrinde(double subtotalProdutos) {
        return subtotalProdutos > 500.00;
    }
}
