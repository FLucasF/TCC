package br.tcc.checkout.dto;

import java.util.List;

public record CheckoutRequisicaoDto(
    List<ItemDto> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas
) {}
