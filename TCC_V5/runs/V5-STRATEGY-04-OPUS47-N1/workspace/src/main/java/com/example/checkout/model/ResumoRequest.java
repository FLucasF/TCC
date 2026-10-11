package com.example.checkout.model;

import java.util.List;

public record ResumoRequest(
    List<Item> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas,
    String nivelClube,
    String regiao
) {}
