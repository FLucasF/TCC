package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public record Pedido(
    List<Item> itens,
    BigDecimal subtotal,
    BigDecimal pesoKg,
    ModalidadeEntrega modalidade,
    Optional<Cupom> cupom,
    FormaPagamento formaPagamento,
    int parcelas,
    NivelClube nivelClube,
    Regiao regiao
) {}
