package br.tcc.checkout.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ResumoCheckoutRequest(
    List<ItemDto> itens,
    @JsonProperty("modalidadeEntrega")
    String modalidadeEntrega,
    String cupom,
    @JsonProperty("formaPagamento")
    String formaPagamento,
    Integer parcelas
) {}
