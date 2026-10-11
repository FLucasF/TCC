package com.loja.checkout.validation;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;

public class PedidoValidator {

    public static void validarItens(PedidoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        for (ItemPedido item : pedido.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario() <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg() <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    public static NivelClube validarNivelClube(String nivel) {
        if (nivel == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        try {
            return NivelClube.valueOf(nivel);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
    }

    public static Regiao validarRegiao(String regiao) {
        if (regiao == null) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }

        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
    }
}
