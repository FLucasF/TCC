package com.loja.checkout.entrega;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Expressa implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public ResultadoEntrega calcular(BigDecimal pesoTotalKg) {
        BigDecimal frete = Arredondamento.centavos(
                new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoTotalKg))
        );
        return new ResultadoEntrega(frete, 2);
    }
}
