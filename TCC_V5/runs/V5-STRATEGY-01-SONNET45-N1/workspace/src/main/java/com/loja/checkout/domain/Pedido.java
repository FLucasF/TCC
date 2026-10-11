package com.loja.checkout.domain;

import java.util.List;

public record Pedido(
    List<Item> itens,
    String codigoCupom,
    int parcelas,
    Regiao regiao
) {}
