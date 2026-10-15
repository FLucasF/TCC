package com.loja.checkout.domain;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public enum Cupom {
    BEMVINDO10("BEMVINDO10") {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
            return subtotalProdutos.multiply(new BigDecimal("0.10"))
                    .setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos) {
            return true;
        }
    },
    MENOS50("MENOS50") {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
            return new BigDecimal("50.00");
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }
    },
    FRETEGRATIS("FRETEGRATIS") {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos) {
            return true;
        }
    },
    LEVE3PAGUE2("LEVE3PAGUE2") {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                if (item.getQuantidade() >= 3 && item.getPrecoUnitario() != null) {
                    int gratuitas = item.getQuantidade() / 3;
                    BigDecimal descontoItem = item.getPrecoUnitario()
                            .multiply(new BigDecimal(gratuitas))
                            .setScale(2, java.math.RoundingMode.HALF_EVEN);
                    desconto = desconto.add(descontoItem);
                }
            }
            return desconto;
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos) {
            return true;
        }
    };

    private final String codigo;

    Cupom(String codigo) {
        this.codigo = codigo;
    }

    public abstract BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens);

    public abstract boolean isAplicavel(BigDecimal subtotalProdutos);

    public static Cupom fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Cupom cupom : values()) {
            if (cupom.codigo.equals(codigo)) {
                return cupom;
            }
        }
        return null;
    }

    public String getCodigo() {
        return codigo;
    }
}
