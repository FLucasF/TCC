package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ContextoPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete) {
        return contexto.subtotalProdutos().multiply(PERCENTUAL);
    }
}
