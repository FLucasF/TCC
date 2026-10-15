package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RessumoRequest {
    @JsonProperty("itens")
    public List<Item> itens;

    @JsonProperty("modalidadeEntrega")
    public ModalidadeEntrega modalidadeEntrega;

    @JsonProperty("cupom")
    public String cupom;

    @JsonProperty("formaPagamento")
    public FormaPagamento formaPagamento;

    @JsonProperty("parcelas")
    public Integer parcelas = 1;

    @JsonProperty("nivelClube")
    public NivelClube nivelClube;

    @JsonProperty("regiao")
    public Regiao regiao;

    public RessumoRequest() {}
}
