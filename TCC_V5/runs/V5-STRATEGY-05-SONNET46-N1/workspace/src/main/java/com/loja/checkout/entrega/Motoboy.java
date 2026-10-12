package com.loja.checkout.entrega;

import com.loja.checkout.CheckoutException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal FRETE = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String codigo() { return "MOTOBOY"; }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return FRETE;
    }

    @Override
    public int prazoEntregaDias() { return 0; }

    @Override
    public void validarDisponibilidade(BigDecimal pesoTotalKg) {
        if (pesoTotalKg.compareTo(PESO_MAXIMO) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }
}
