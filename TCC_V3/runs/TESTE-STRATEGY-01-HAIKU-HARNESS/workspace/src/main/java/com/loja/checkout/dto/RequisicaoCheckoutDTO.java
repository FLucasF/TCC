package com.loja.checkout.dto;

import java.util.List;

public class RequisicaoCheckoutDTO {
    public List<ItemDTO> itens;
    public String modalidadeEntrega;
    public String cupom;
    public String formaPagamento;
    public Integer parcelas;
    public String nivelClube;
    public String regiao;

    public RequisicaoCheckoutDTO() {
        this.parcelas = 1;
        this.nivelClube = "BRONZE";
    }
}
