package com.lojaroupas.enums;

import java.util.List;
import com.lojaroupas.dto.ItemCarrinho;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public Double aplicarDesconto(Double subtotal, List<ItemCarrinho> itens) {
            return subtotal * 0.10;
        }

        @Override
        public boolean validaParaPedido(Double subtotal) {
            return true;
        }
    },
    MENOS50 {
        @Override
        public Double aplicarDesconto(Double subtotal, List<ItemCarrinho> itens) {
            return 50.00;
        }

        @Override
        public boolean validaParaPedido(Double subtotal) {
            return subtotal >= 300.00;
        }
    },
    FRETEGRATIS {
        @Override
        public Double aplicarDesconto(Double subtotal, List<ItemCarrinho> itens) {
            return 0.00;
        }

        @Override
        public boolean validaParaPedido(Double subtotal) {
            return true;
        }

        @Override
        public boolean ehFreteGratis() {
            return true;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public Double aplicarDesconto(Double subtotal, List<ItemCarrinho> itens) {
            Double desconto = 0.0;
            for (ItemCarrinho item : itens) {
                int quantidade = item.quantidade;
                int gruposCompletos = quantidade / 3;
                desconto += gruposCompletos * item.precoUnitario;
            }
            return desconto;
        }

        @Override
        public boolean validaParaPedido(Double subtotal) {
            return true;
        }

        @Override
        public boolean ehLeve3Pague2() {
            return true;
        }
    };

    public abstract Double aplicarDesconto(Double subtotal, List<ItemCarrinho> itens);
    public abstract boolean validaParaPedido(Double subtotal);

    public boolean ehFreteGratis() {
        return false;
    }

    public boolean ehLeve3Pague2() {
        return false;
    }

    public static Cupom fromString(String value) {
        try {
            return Cupom.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
