package com.loja.checkout.cupom;

import com.loja.checkout.domain.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(PedidoContext contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContext contexto) {
        return contexto.frete();
    }
}
