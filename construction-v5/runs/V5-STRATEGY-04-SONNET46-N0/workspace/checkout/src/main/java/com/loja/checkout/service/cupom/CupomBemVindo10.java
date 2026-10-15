package com.loja.checkout.service.cupom;

import com.loja.checkout.service.Arredondamento;
import org.springframework.stereotype.Component;

@Component
public class CupomBemVindo10 implements Cupom {
    @Override
    public String getCodigo() { return "BEMVINDO10"; }

    @Override
    public boolean aplicavel(ContextoCupom ctx) { return true; }

    @Override
    public ResultadoCupom calcular(ContextoCupom ctx) {
        double desconto = Arredondamento.arredondar(ctx.subtotalProdutos() * 0.10);
        return new ResultadoCupom(desconto, false);
    }
}
