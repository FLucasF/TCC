package br.tcc.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CheckoutRequest {
    @JsonProperty("itens")
    public List<ItemRequest> itens;

    @JsonProperty("modalidadeEntrega")
    public String modalidadeEntrega;

    @JsonProperty("cupom")
    public String cupom;

    @JsonProperty("formaPagamento")
    public String formaPagamento;

    @JsonProperty("parcelas")
    public Integer parcelas;
}
