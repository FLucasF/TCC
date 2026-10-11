package com.loja.checkout.entrega;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public ResultadoEntrega calcular(BigDecimal pesoTotalKg) {
        BigDecimal frete = Arredondamento.centavos(
                new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoTotalKg))
        );
        return new ResultadoEntrega(frete, 7);
    }
}
