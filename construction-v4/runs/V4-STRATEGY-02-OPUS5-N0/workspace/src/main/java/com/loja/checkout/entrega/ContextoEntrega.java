package com.loja.checkout.entrega;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Regiao;

/** Dados do pedido disponíveis para a opção de entrega calcular o frete e o prazo. */
public record ContextoEntrega(Carrinho carrinho, Regiao regiao, NivelClube nivelClube) {
}
