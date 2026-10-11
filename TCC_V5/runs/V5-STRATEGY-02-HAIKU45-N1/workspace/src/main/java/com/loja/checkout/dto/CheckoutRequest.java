package com.loja.checkout.dto;

import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;

import java.util.List;

public record CheckoutRequest(
    List<ItemRequest> itens,
    ModalidadeEntrega modalidadeEntrega,
    String cupom,
    FormaPagamento formaPagamento,
    Integer parcelas,
    NivelClube nivelClube,
    Regiao regiao
) {}
