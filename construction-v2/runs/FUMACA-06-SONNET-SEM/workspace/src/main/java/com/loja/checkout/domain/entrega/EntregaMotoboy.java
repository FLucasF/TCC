package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR_FIXO = new BigDecimal("18.00");
    private static final BigDecimal LIMITE_PESO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return VALOR_FIXO;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoTotalKg) {
        if (pesoTotalKg.compareTo(LIMITE_PESO_KG) > 0) {
            indisponivel();
        }
    }
}
