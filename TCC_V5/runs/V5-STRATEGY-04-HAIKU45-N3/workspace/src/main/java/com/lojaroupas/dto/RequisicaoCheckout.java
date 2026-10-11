package com.lojaroupas.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RequisicaoCheckout {
    @SerializedName("itens")
    public List<ItemCarrinho> itens;

    @SerializedName("modalidadeEntrega")
    public String modalidadeEntrega;

    @SerializedName("cupom")
    public String cupom;

    @SerializedName("formaPagamento")
    public String formaPagamento;

    @SerializedName("parcelas")
    public Integer parcelas;

    @SerializedName("nivelClube")
    public String nivelClube;

    @SerializedName("regiao")
    public String regiao;

    public RequisicaoCheckout() {
        this.parcelas = 1;
    }

    public Integer getParcelas() {
        return parcelas != null ? parcelas : 1;
    }
}
