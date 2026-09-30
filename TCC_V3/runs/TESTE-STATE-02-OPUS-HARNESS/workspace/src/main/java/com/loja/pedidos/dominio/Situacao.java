package com.loja.pedidos.dominio;

import java.util.Map;
import java.util.Optional;

import static com.loja.pedidos.dominio.Efeito.AGENDA_A_COLETA;
import static com.loja.pedidos.dominio.Efeito.DEVOLVE_O_ESTOQUE;
import static com.loja.pedidos.dominio.Efeito.REEMBOLSA_O_TOTAL;
import static com.loja.pedidos.dominio.Efeito.REEMBOLSA_O_TOTAL_MENOS_A_TAXA_DE_SEPARACAO;
import static com.loja.pedidos.dominio.Efeito.REEMBOLSA_SO_OS_PRODUTOS;

/** Cada situacao do pedido: o texto que o cliente ve e o que cada acao faz a partir dela. */
public enum Situacao {

    AGUARDANDO_PAGAMENTO("Aguardando pagamento") {
        @Override
        protected Map<Acao, Transicao> transicoes() {
            return Map.of(
                    Acao.PAGAR, new Transicao(PAGO),
                    Acao.CANCELAR, new Transicao(CANCELADO));
        }
    },

    PAGO("Pagamento confirmado") {
        @Override
        protected Map<Acao, Transicao> transicoes() {
            return Map.of(
                    Acao.SEPARAR, new Transicao(EM_SEPARACAO),
                    Acao.CANCELAR, new Transicao(CANCELADO, REEMBOLSA_O_TOTAL));
        }
    },

    EM_SEPARACAO("Separando seus produtos") {
        @Override
        protected Map<Acao, Transicao> transicoes() {
            return Map.of(
                    Acao.ENVIAR, new Transicao(ENVIADO),
                    Acao.CANCELAR, new Transicao(CANCELADO,
                            REEMBOLSA_O_TOTAL_MENOS_A_TAXA_DE_SEPARACAO.e(DEVOLVE_O_ESTOQUE)));
        }
    },

    ENVIADO("A caminho") {
        @Override
        protected Map<Acao, Transicao> transicoes() {
            return Map.of(Acao.ENTREGAR, new Transicao(ENTREGUE));
        }
    },

    ENTREGUE("Entregue") {
        @Override
        protected Map<Acao, Transicao> transicoes() {
            return Map.of(Acao.DEVOLVER, new Transicao(DEVOLVIDO,
                    REEMBOLSA_SO_OS_PRODUTOS.e(AGENDA_A_COLETA)));
        }
    },

    CANCELADO("Cancelado") {
        @Override
        protected Map<Acao, Transicao> transicoes() {
            return Map.of();
        }
    },

    DEVOLVIDO("Devolvido") {
        @Override
        protected Map<Acao, Transicao> transicoes() {
            return Map.of();
        }
    };

    private final String descricao;

    Situacao(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public Optional<Transicao> transicaoPara(Acao acao) {
        return Optional.ofNullable(transicoes().get(acao));
    }

    protected abstract Map<Acao, Transicao> transicoes();
}
