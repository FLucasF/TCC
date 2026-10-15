package com.loja.pedidos;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cada situação sabe como aparece para o cliente e o que cada ação faz a partir dela.
 * Ação não mapeada aqui não é permitida naquela situação.
 */
public enum Situacao {

    AGUARDANDO_PAGAMENTO("Aguardando pagamento") {
        @Override
        protected void definirTransicoes(Transicoes transicoes) {
            transicoes.de(Acao.PAGAR, PAGO, Efeito.NENHUM);
            transicoes.de(Acao.CANCELAR, CANCELADO, Efeito.NENHUM);
        }
    },

    PAGO("Pagamento confirmado") {
        @Override
        protected void definirTransicoes(Transicoes transicoes) {
            transicoes.de(Acao.SEPARAR, EM_SEPARACAO, Efeito.NENHUM);
            transicoes.de(Acao.CANCELAR, CANCELADO, Efeito.REEMBOLSAR_TOTAL);
        }
    },

    EM_SEPARACAO("Separando seus produtos") {
        @Override
        protected void definirTransicoes(Transicoes transicoes) {
            transicoes.de(Acao.ENVIAR, ENVIADO, Efeito.NENHUM);
            transicoes.de(Acao.CANCELAR, CANCELADO, Efeito.REEMBOLSAR_TOTAL_MENOS_TAXA_DE_SEPARACAO);
        }
    },

    ENVIADO("A caminho") {
        @Override
        protected void definirTransicoes(Transicoes transicoes) {
            transicoes.de(Acao.ENTREGAR, ENTREGUE, Efeito.NENHUM);
        }
    },

    ENTREGUE("Entregue") {
        @Override
        protected void definirTransicoes(Transicoes transicoes) {
            transicoes.de(Acao.DEVOLVER, DEVOLVIDO, Efeito.REEMBOLSAR_PRODUTOS_E_AGENDAR_COLETA);
        }
    },

    CANCELADO("Cancelado") {
        @Override
        protected void definirTransicoes(Transicoes transicoes) {
        }
    },

    DEVOLVIDO("Devolvido") {
        @Override
        protected void definirTransicoes(Transicoes transicoes) {
        }
    };

    private final String descricao;
    private Map<Acao, Transicao> transicoes;

    Situacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** A transição da ação pedida, ou vazio quando a ação não vale nesta situação. */
    public Transicao transicaoPara(Acao acao) {
        if (transicoes == null) {
            Transicoes coletadas = new Transicoes();
            definirTransicoes(coletadas);
            transicoes = coletadas.mapa;
        }
        return transicoes.get(acao);
    }

    protected abstract void definirTransicoes(Transicoes transicoes);

    protected static final class Transicoes {
        private final Map<Acao, Transicao> mapa = new EnumMap<>(Acao.class);

        void de(Acao acao, Situacao destino, Efeito efeito) {
            mapa.put(acao, new Transicao(destino, efeito));
        }
    }
}
