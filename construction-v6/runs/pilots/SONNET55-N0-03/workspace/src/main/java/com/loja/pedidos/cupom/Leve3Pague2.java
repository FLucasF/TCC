package com.loja.pedidos.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Dinheiro;
import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;

@Component
public class Leve3Pague2 implements Cupom {

    public String codigo() {
        return "LEVE3PAGUE2";
    }

    public BigDecimal desconto(ContextoCupom ctx) {
        BigDecimal desconto = Dinheiro.ZERO;
        for (LinhaItem item : ctx.itens()) {
            int gratis = item.quantidade() / 3;
            desconto = desconto.add(item.produto().preco().multiply(BigDecimal.valueOf(gratis)));
        }
        if (desconto.signum() == 0) {
            throw new ErroNegocio(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return desconto;
    }
}
