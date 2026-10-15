package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NivelBronze implements BeneficioClube {

    @Override
    public String getCodigo() {
        return "BRONZE";
    }

    @Override
    public boolean isFreteGratis() {
        return false;
    }

    @Override
    public boolean isJurosSemAte6x() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean temBrinde(BigDecimal subtotal) {
        return false;
    }
}
