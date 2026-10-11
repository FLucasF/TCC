package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ContextoPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete) {
        return frete;
    }
}
