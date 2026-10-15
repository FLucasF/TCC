package com.loja.checkout.model;

public enum Region {
    SUDESTE(0.01),
    SUL(0.01),
    CENTRO_OESTE(0.015),
    NORTE(0.025),
    NORDESTE(0.02);

    private final double insurancePercentage;

    Region(double insurancePercentage) {
        this.insurancePercentage = insurancePercentage;
    }

    public double getInsurancePercentage() {
        return insurancePercentage;
    }
}
