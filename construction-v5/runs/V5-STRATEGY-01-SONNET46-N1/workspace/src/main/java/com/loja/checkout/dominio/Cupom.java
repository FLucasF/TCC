package com.loja.checkout.dominio;

import com.loja.checkout.api.CheckoutRequest.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static com.loja.checkout.dominio.CodigoErro.CUPOM_NAO_APLICAVEL;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public void validarAplicabilidade(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {}

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        }
    },

    MENOS50 {
        @Override
        public void validarAplicabilidade(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            if (subtotal.compareTo(new BigDecimal("300")) < 0)
                throw new CheckoutException(CUPOM_NAO_APLICAVEL);
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public void validarAplicabilidade(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {}

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public void validarAplicabilidade(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {}

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
            return itens.stream()
                    .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
    };

    public abstract void validarAplicabilidade(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens);

    public abstract BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens);

    public static Optional<Cupom> fromCodigo(String codigo) {
        try {
            return Optional.of(valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
