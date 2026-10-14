package com.loja.pedidos.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Dinheiro;
import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;
import com.loja.pedidos.persistencia.PedidoRepositorio;

@Component
public class BemVindo10 implements Cupom {

    private final PedidoRepositorio pedidos;

    public BemVindo10(PedidoRepositorio pedidos) {
        this.pedidos = pedidos;
    }

    public String codigo() {
        return "BEMVINDO10";
    }

    public BigDecimal desconto(ContextoCupom ctx) {
        if (pedidos.possuiCompraConfirmada(ctx.cpf())) {
            throw new ErroNegocio(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.fracao(ctx.subtotal(), new BigDecimal("0.10"));
    }
}
