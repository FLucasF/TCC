package com.loja.resumo.entrega;

import com.loja.resumo.dinheiro.Dinheiro;
import com.loja.resumo.dominio.Pedido;

import java.math.BigDecimal;

public enum ModalidadeEntregaPadrao implements ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal calcularFrete(Pedido pedido) {
            return Dinheiro.arredondar(new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pedido.pesoTotal())));
        }

        @Override
        public int prazoDias() {
            return 7;
        }

        @Override
        public boolean disponivelPara(Pedido pedido) {
            return true;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal calcularFrete(Pedido pedido) {
            return Dinheiro.arredondar(new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pedido.pesoTotal())));
        }

        @Override
        public int prazoDias() {
            return 2;
        }

        @Override
        public boolean disponivelPara(Pedido pedido) {
            return true;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularFrete(Pedido pedido) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public int prazoDias() {
            return 1;
        }

        @Override
        public boolean disponivelPara(Pedido pedido) {
            return true;
        }
    },

    MOTOBOY {
        @Override
        public BigDecimal calcularFrete(Pedido pedido) {
            return new BigDecimal("18.00");
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean disponivelPara(Pedido pedido) {
            return pedido.pesoTotal().compareTo(PESO_MAXIMO_MOTOBOY_KG) <= 0;
        }
    };

    private static final BigDecimal PESO_MAXIMO_MOTOBOY_KG = new BigDecimal("5");
}
