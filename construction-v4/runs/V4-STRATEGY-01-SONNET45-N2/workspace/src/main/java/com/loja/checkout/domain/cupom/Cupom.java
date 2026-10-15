package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.exception.CheckoutException;

import java.math.BigDecimal;
import java.util.List;

import static com.loja.checkout.util.Moeda.arredondar;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
            return true;
        }

        @Override
        public ResultadoCupom calcular(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
            BigDecimal desconto = arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
            return new ResultadoCupom(desconto);
        }
    },
    MENOS50 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public ResultadoCupom calcular(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
            if (!aplicavel(subtotalProdutos, itens, frete)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            return new ResultadoCupom(new BigDecimal("50.00"));
        }
    },
    FRETEGRATIS {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
            return true;
        }

        @Override
        public ResultadoCupom calcular(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
            return new ResultadoCupom(frete);
        }
    },
    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
            return true;
        }

        @Override
        public ResultadoCupom calcular(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
            BigDecimal descontoTotal = BigDecimal.ZERO;
            for (ItemCarrinho item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                BigDecimal descontoItem = item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis));
                descontoTotal = descontoTotal.add(descontoItem);
            }
            return new ResultadoCupom(arredondar(descontoTotal));
        }
    };

    public abstract boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete);
    public abstract ResultadoCupom calcular(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete);
}
