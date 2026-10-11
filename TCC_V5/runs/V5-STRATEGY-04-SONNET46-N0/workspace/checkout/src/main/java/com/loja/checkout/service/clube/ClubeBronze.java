package com.loja.checkout.service.clube;

import org.springframework.stereotype.Component;

@Component
public class ClubeBronze implements BeneficioClube {
    @Override
    public String getNivel() { return "BRONZE"; }

    @Override
    public boolean isFreteGratis() { return false; }

    @Override
    public double calcularCredito(double subtotalProdutos) { return 0.00; }

    @Override
    public boolean temBrinde(double subtotalProdutos) { return false; }
}
