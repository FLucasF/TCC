package com.loja.checkout.entrega;

import com.loja.checkout.exception.CheckoutException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaMotoboy implements CalculoEntrega {

    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public void validar(BigDecimal pesoKg) {
        if (pesoKg.compareTo(PESO_MAXIMO) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    @Override
    public ResultadoEntrega calcular(BigDecimal pesoKg) {
        return new ResultadoEntrega(new BigDecimal("18.00"), 0);
    }
}
