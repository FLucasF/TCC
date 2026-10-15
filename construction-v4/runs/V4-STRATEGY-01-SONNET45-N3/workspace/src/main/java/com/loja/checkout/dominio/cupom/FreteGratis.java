package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ContextoPedido;
import java.math.BigDecimal;

public class FreteGratis implements Cupom {

    @Override
    public boolean podeAplicar(ContextoPedido contexto, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete) {
        return frete;
    }
}
