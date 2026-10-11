package com.loja.checkout.service.clube;

import com.loja.checkout.service.Arredondamento;
import org.springframework.stereotype.Component;

@Component
public class ClubePrata implements BeneficioClube {
    @Override
    public String getNivel() { return "PRATA"; }

    @Override
    public boolean isFreteGratis() { return false; }

    @Override
    public double calcularCredito(double subtotalProdutos) {
        return Arredondamento.arredondar(subtotalProdutos * 0.02);
    }

    @Override
    public boolean temBrinde(double subtotalProdutos) { return false; }
}
