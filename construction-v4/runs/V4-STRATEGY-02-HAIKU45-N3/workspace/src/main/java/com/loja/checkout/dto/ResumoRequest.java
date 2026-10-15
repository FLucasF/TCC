package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ResumoRequest(
    @JsonProperty("itens") List<ItemRequest> itens,
    @JsonProperty("modalidadeEntrega") String modalidadeEntrega,
    @JsonProperty("cupom") String cupom,
    @JsonProperty("formaPagamento") String formaPagamento,
    @JsonProperty("parcelas") Integer parcelas,
    @JsonProperty("nivelClube") String nivelClube,
    @JsonProperty("regiao") String regiao
) {}
