package com.loja.checkout.model;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.dto.ItemCarrinho;

public enum Cupom {
    BEMVINDO10("BEMVINDO10"),
    MENOS50("MENOS50"),
    FRETEGRATIS("FRETEGRATIS"),
    LEVE3PAGUE2("LEVE3PAGUE2");

    private final String codigo;

    Cupom(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public boolean ehAplicavel(BigDecimal subtotal, BigDecimal pesoTotal, List<ItemCarrinho> itens) {
        if (this == MENOS50) {
            return subtotal.compareTo(BigDecimal.valueOf(300.00)) >= 0;
        }
        return true;
    }

    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemCarrinho> itens, BigDecimal frete) {
        return switch (this) {
            case BEMVINDO10 -> subtotal.multiply(new BigDecimal("0.10"));
            case MENOS50 -> new BigDecimal("50.00");
            case FRETEGRATIS -> frete;
            case LEVE3PAGUE2 -> calcularDescontoLeve3Pague2(itens);
        };
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            if (item.getQuantidade() >= 3) {
                int gratuitos = item.getQuantidade() / 3;
                BigDecimal descontoItem = item.getPrecoUnitario().multiply(BigDecimal.valueOf(gratuitos));
                desconto = desconto.add(descontoItem);
            }
        }
        return desconto;
    }
}
