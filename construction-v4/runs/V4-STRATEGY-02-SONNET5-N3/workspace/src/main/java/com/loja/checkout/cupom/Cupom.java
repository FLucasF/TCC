package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.util.Optional;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(CupomContexto contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(CupomContexto contexto) {
            return Dinheiro.arredondar(contexto.subtotal().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(CupomContexto contexto) {
            return contexto.subtotal().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(CupomContexto contexto) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(CupomContexto contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(CupomContexto contexto) {
            return contexto.frete();
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(CupomContexto contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(CupomContexto contexto) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : contexto.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(desconto);
        }
    };

    public abstract boolean aplicavel(CupomContexto contexto);

    public abstract BigDecimal calcularDesconto(CupomContexto contexto);

    public static Optional<Cupom> fromCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
