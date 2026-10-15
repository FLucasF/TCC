package com.loja.checkout.service.cupom;

import java.math.BigDecimal;
import java.util.List;

import com.loja.checkout.model.Item;
import com.loja.checkout.service.Arredondamento;

public sealed interface EstrategiaCupom {
    ResultadoCupom aplicar(BigDecimal subtotal, BigDecimal frete, List<Item> itens);

    boolean valido(BigDecimal subtotal);

    record Bemvindo10() implements EstrategiaCupom {
        @Override
        public ResultadoCupom aplicar(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
            BigDecimal desconto = Arredondamento.arredondar(subtotal.multiply(new BigDecimal("0.10")));
            return new ResultadoCupom(desconto, BigDecimal.ZERO);
        }

        @Override
        public boolean valido(BigDecimal subtotal) {
            return true;
        }
    }

    record Menos50() implements EstrategiaCupom {
        private static final BigDecimal MINIMO = new BigDecimal("300.00");

        @Override
        public ResultadoCupom aplicar(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
            BigDecimal desconto = new BigDecimal("50.00");
            return new ResultadoCupom(desconto, BigDecimal.ZERO);
        }

        @Override
        public boolean valido(BigDecimal subtotal) {
            return subtotal.compareTo(MINIMO) >= 0;
        }
    }

    record FreteGratis() implements EstrategiaCupom {
        @Override
        public ResultadoCupom aplicar(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
            BigDecimal desconto = Arredondamento.arredondar(frete);
            return new ResultadoCupom(desconto, desconto);
        }

        @Override
        public boolean valido(BigDecimal subtotal) {
            return true;
        }
    }

    record Leve3Pague2() implements EstrategiaCupom {
        @Override
        public ResultadoCupom aplicar(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
            BigDecimal desconto = BigDecimal.ZERO;

            for (Item item : itens) {
                int quantidade = item.quantidade();
                int gratuitas = quantidade / 3;
                BigDecimal descontoItem = new BigDecimal(gratuitas)
                    .multiply(item.precoUnitario());
                desconto = desconto.add(descontoItem);
            }

            desconto = Arredondamento.arredondar(desconto);
            return new ResultadoCupom(desconto, BigDecimal.ZERO);
        }

        @Override
        public boolean valido(BigDecimal subtotal) {
            return true;
        }
    }
}
