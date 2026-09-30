package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.Optional;

public enum Situacao {

    AGUARDANDO_PAGAMENTO("Aguardando pagamento") {
        @Override
        Optional<Efeito> aplicar(Acao acao, BigDecimal valorProdutos, BigDecimal frete, BigDecimal valorTotal) {
            return switch (acao) {
                case PAGAR -> Optional.of(Efeito.transicaoSimples(PAGO));
                case CANCELAR -> Optional.of(Efeito.cancelamento(BigDecimal.ZERO, false));
                default -> Optional.empty();
            };
        }
    },

    PAGO("Pagamento confirmado") {
        @Override
        Optional<Efeito> aplicar(Acao acao, BigDecimal valorProdutos, BigDecimal frete, BigDecimal valorTotal) {
            return switch (acao) {
                case SEPARAR -> Optional.of(Efeito.transicaoSimples(EM_SEPARACAO));
                case CANCELAR -> Optional.of(Efeito.cancelamento(valorTotal, false));
                default -> Optional.empty();
            };
        }
    },

    EM_SEPARACAO("Separando seus produtos") {
        private final BigDecimal taxaSeparacao = new BigDecimal("15.00");

        @Override
        Optional<Efeito> aplicar(Acao acao, BigDecimal valorProdutos, BigDecimal frete, BigDecimal valorTotal) {
            return switch (acao) {
                case ENVIAR -> Optional.of(Efeito.transicaoSimples(ENVIADO));
                case CANCELAR -> {
                    BigDecimal reembolso = valorTotal.subtract(taxaSeparacao).max(BigDecimal.ZERO);
                    yield Optional.of(Efeito.cancelamento(reembolso, true));
                }
                default -> Optional.empty();
            };
        }
    },

    ENVIADO("A caminho") {
        @Override
        Optional<Efeito> aplicar(Acao acao, BigDecimal valorProdutos, BigDecimal frete, BigDecimal valorTotal) {
            return switch (acao) {
                case ENTREGAR -> Optional.of(Efeito.transicaoSimples(ENTREGUE));
                default -> Optional.empty();
            };
        }
    },

    ENTREGUE("Entregue") {
        @Override
        Optional<Efeito> aplicar(Acao acao, BigDecimal valorProdutos, BigDecimal frete, BigDecimal valorTotal) {
            return switch (acao) {
                case DEVOLVER -> Optional.of(Efeito.devolucao(valorProdutos));
                default -> Optional.empty();
            };
        }
    },

    CANCELADO("Cancelado") {
        @Override
        Optional<Efeito> aplicar(Acao acao, BigDecimal valorProdutos, BigDecimal frete, BigDecimal valorTotal) {
            return Optional.empty();
        }
    },

    DEVOLVIDO("Devolvido") {
        @Override
        Optional<Efeito> aplicar(Acao acao, BigDecimal valorProdutos, BigDecimal frete, BigDecimal valorTotal) {
            return Optional.empty();
        }
    };

    private final String descricao;

    Situacao(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    abstract Optional<Efeito> aplicar(Acao acao, BigDecimal valorProdutos, BigDecimal frete, BigDecimal valorTotal);
}
