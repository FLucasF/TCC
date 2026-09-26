package com.loja.domain;

import com.loja.dto.Item;
import java.util.List;

public abstract class Cupom {
    protected String codigo;

    public Cupom(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public abstract boolean ehAplicavel(Double subtotalProdutos, Double totalComFrete);
    public abstract CupomResultado calcular(Double subtotalProdutos, Double frete, List<Item> itens);

    public static class CupomResultado {
        public Double desconto;
        public Double frete;

        public CupomResultado(Double desconto, Double frete) {
            this.desconto = desconto;
            this.frete = frete;
        }
    }
}
