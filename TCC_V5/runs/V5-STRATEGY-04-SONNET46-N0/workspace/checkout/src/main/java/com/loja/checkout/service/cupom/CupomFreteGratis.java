package com.loja.checkout.service.cupom;

import org.springframework.stereotype.Component;

@Component
public class CupomFreteGratis implements Cupom {
    @Override
    public String getCodigo() { return "FRETEGRATIS"; }

    @Override
    public boolean aplicavel(ContextoCupom ctx) { return true; }

    @Override
    public ResultadoCupom calcular(ContextoCupom ctx) {
        return new ResultadoCupom(ctx.frete(), true);
    }
}
