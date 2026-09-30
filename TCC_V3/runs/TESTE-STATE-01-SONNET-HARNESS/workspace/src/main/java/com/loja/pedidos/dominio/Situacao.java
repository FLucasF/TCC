package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cada situação sabe, sozinha, quais ações aceita e o que cada uma faz.
 * Escolher o efeito de uma ação é uma consulta ao mapa da situação atual,
 * não uma sequência de condições sobre a situação.
 */
public enum Situacao {

    AGUARDANDO_PAGAMENTO("Aguardando pagamento") {
        @Override
        public Map<Acao, Transicao> transicoes() {
            return Map.of(
                    Acao.PAGAR, pedido -> Efeito.mudarPara(PAGO),
                    Acao.CANCELAR, pedido -> Efeito.mudarPara(CANCELADO));
        }
    },

    PAGO("Pagamento confirmado") {
        @Override
        public Map<Acao, Transicao> transicoes() {
            return Map.of(
                    Acao.SEPARAR, pedido -> Efeito.mudarPara(EM_SEPARACAO),
                    Acao.CANCELAR, pedido -> Efeito.mudarPara(CANCELADO)
                            .comReembolso(pedido.getValorTotal()));
        }
    },

    EM_SEPARACAO("Separando seus produtos") {
        @Override
        public Map<Acao, Transicao> transicoes() {
            return Map.of(
                    Acao.ENVIAR, pedido -> Efeito.mudarPara(ENVIADO),
                    Acao.CANCELAR, pedido -> Efeito.mudarPara(CANCELADO)
                            .comReembolso(reembolsoMenosTaxaDeSeparacao(pedido))
                            .comEstoqueDevolvido());
        }
    },

    ENVIADO("A caminho") {
        @Override
        public Map<Acao, Transicao> transicoes() {
            return Map.of(
                    Acao.ENTREGAR, pedido -> Efeito.mudarPara(ENTREGUE));
        }
    },

    ENTREGUE("Entregue") {
        @Override
        public Map<Acao, Transicao> transicoes() {
            return Map.of(
                    Acao.DEVOLVER, pedido -> Efeito.mudarPara(DEVOLVIDO)
                            .comReembolso(pedido.getValorProdutos())
                            .comColetaAgendada());
        }
    },

    CANCELADO("Cancelado") {
        @Override
        public Map<Acao, Transicao> transicoes() {
            return Map.of();
        }
    },

    DEVOLVIDO("Devolvido") {
        @Override
        public Map<Acao, Transicao> transicoes() {
            return Map.of();
        }
    };

    private static final BigDecimal TAXA_SEPARACAO = new BigDecimal("15.00");

    private final String descricao;

    Situacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public abstract Map<Acao, Transicao> transicoes();

    private static BigDecimal reembolsoMenosTaxaDeSeparacao(Pedido pedido) {
        BigDecimal reembolso = pedido.getValorTotal().subtract(TAXA_SEPARACAO);
        return reembolso.signum() < 0 ? BigDecimal.ZERO : reembolso;
    }
}
