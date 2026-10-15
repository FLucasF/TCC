package com.loja.pedidos.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;

@Component
public class FreteGratis implements Cupom {

    public String codigo() {
        return "FRETEGRATIS";
    }

    public BigDecimal desconto(ContextoCupom ctx) {
        if (!ctx.modalidade().temFrete()) {
            throw new ErroNegocio(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return ctx.frete();
    }
}
