package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Monta o pedido a partir do carrinho enviado, recusando carrinho vazio ou item sem valor valido. */
@Component
public class MontadorDePedido {

    public Pedido montar(List<ItemSolicitado> itens) {
        if (itens == null || itens.isEmpty()) {
            throw ErroCheckout.PEDIDO_INVALIDO.erro();
        }
        return new Pedido(itens.stream().map(this::item).toList());
    }

    private ItemPedido item(ItemSolicitado solicitado) {
        if (solicitado == null) {
            throw ErroCheckout.PEDIDO_INVALIDO.erro();
        }
        return new ItemPedido(
                solicitado.nome(),
                positivo(solicitado.precoUnitario()),
                positivo(solicitado.quantidade()),
                positivo(solicitado.pesoKg()));
    }

    private BigDecimal positivo(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw ErroCheckout.PEDIDO_INVALIDO.erro();
        }
        return valor;
    }

    private int positivo(Integer valor) {
        if (valor == null || valor <= 0) {
            throw ErroCheckout.PEDIDO_INVALIDO.erro();
        }
        return valor;
    }
}
