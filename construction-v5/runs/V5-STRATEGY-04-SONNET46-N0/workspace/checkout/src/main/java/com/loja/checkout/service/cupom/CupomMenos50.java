package com.loja.checkout.service.cupom;

import org.springframework.stereotype.Component;

@Component
public class CupomMenos50 implements Cupom {
    @Override
    public String getCodigo() { return "MENOS50"; }

    @Override
    public boolean aplicavel(ContextoCupom ctx) {
        return ctx.subtotalProdutos() >= 300.00;
    }

    @Override
    public ResultadoCupom calcular(ContextoCupom ctx) {
        return new ResultadoCupom(50.00, false);
    }
}
