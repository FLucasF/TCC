package com.loja.checkout.service.clube;

import com.loja.checkout.model.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Service
public class ClubeService {

    private static final Set<String> NIVEIS_VALIDOS = Set.of("BRONZE", "PRATA", "OURO");

    public void validar(String nivel) {
        if (nivel == null || !NIVEIS_VALIDOS.contains(nivel)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    public boolean freteGratis(String nivel) {
        return "OURO".equals(nivel);
    }

    public boolean temBrinde(String nivel, BigDecimal subtotalProdutos) {
        return "OURO".equals(nivel) && subtotalProdutos.compareTo(BigDecimal.valueOf(500.00)) > 0;
    }

    public BigDecimal calcularCredito(String nivel, BigDecimal subtotalProdutos) {
        return switch (nivel) {
            case "PRATA" -> subtotalProdutos.multiply(BigDecimal.valueOf(0.02))
                    .setScale(2, RoundingMode.HALF_EVEN);
            case "OURO" -> subtotalProdutos.multiply(BigDecimal.valueOf(0.05))
                    .setScale(2, RoundingMode.HALF_EVEN);
            default -> BigDecimal.ZERO.setScale(2);
        };
    }
}
