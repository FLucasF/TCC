package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * As situacoes pelas quais um pedido pode passar. Cada situacao guarda o texto
 * que o cliente ve e as acoes que valem nela, com o que cada acao provoca.
 *
 * <p>Para mudar o processo (uma etapa nova no meio do caminho, por exemplo)
 * basta acrescentar a situacao aqui e declarar as suas transicoes: nao existe
 * regra de fluxo espalhada em outro lugar do servico.
 */
public enum Situacao {

    AGUARDANDO_PAGAMENTO("Aguardando pagamento") {
        @Override
        protected void declararTransicoes(Map<Acao, Transicao> transicoes) {
            transicoes.put(Acao.PAGAR, Transicao.para(PAGO));
            // Ainda nao pagou, entao nao ha nada para devolver ao cliente.
            transicoes.put(Acao.CANCELAR, Transicao.para(CANCELADO));
        }
    },

    PAGO("Pagamento confirmado") {
        @Override
        protected void declararTransicoes(Map<Acao, Transicao> transicoes) {
            transicoes.put(Acao.SEPARAR, Transicao.para(EM_SEPARACAO));
            transicoes.put(Acao.CANCELAR, Transicao.para(CANCELADO)
                    .comReembolso(Pedido::valorTotal));
        }
    },

    EM_SEPARACAO("Separando seus produtos") {
        @Override
        protected void declararTransicoes(Map<Acao, Transicao> transicoes) {
            transicoes.put(Acao.ENVIAR, Transicao.para(ENVIADO));
            // Cancelar depois de separado cobra a taxa de separacao e os
            // produtos ja separados voltam para o estoque na hora.
            transicoes.put(Acao.CANCELAR, Transicao.para(CANCELADO)
                    .comReembolso(pedido -> Dinheiro.subtraiAteZero(pedido.valorTotal(), TAXA_SEPARACAO))
                    .devolvendoEstoque());
        }
    },

    ENVIADO("A caminho") {
        @Override
        protected void declararTransicoes(Map<Acao, Transicao> transicoes) {
            // Depois de sair com a transportadora nao da mais para cancelar:
            // o cliente devolve depois de receber.
            transicoes.put(Acao.ENTREGAR, Transicao.para(ENTREGUE));
        }
    },

    ENTREGUE("Entregue") {
        @Override
        protected void declararTransicoes(Map<Acao, Transicao> transicoes) {
            // Na devolucao o frete nao volta e o pacote e buscado na casa do
            // cliente, entao o estoque so volta quando a coleta chegar.
            transicoes.put(Acao.DEVOLVER, Transicao.para(DEVOLVIDO)
                    .comReembolso(Pedido::valorProdutos)
                    .agendandoColeta());
        }
    },

    CANCELADO("Cancelado"),

    DEVOLVIDO("Devolvido");

    /** Taxa cobrada do cliente quando o cancelamento acontece na separacao. */
    public static final BigDecimal TAXA_SEPARACAO = Dinheiro.valor(new BigDecimal("15.00"));

    private final String descricao;
    private Map<Acao, Transicao> transicoes;

    Situacao(String descricao) {
        this.descricao = descricao;
    }

    /** Texto que o cliente ve no site para esta situacao. */
    public String descricao() {
        return descricao;
    }

    /** A transicao da acao, vazia quando a acao nao vale nesta situacao. */
    public Optional<Transicao> transicao(Acao acao) {
        return Optional.ofNullable(transicoes().get(acao));
    }

    private Map<Acao, Transicao> transicoes() {
        if (transicoes == null) {
            Map<Acao, Transicao> declaradas = new EnumMap<>(Acao.class);
            declararTransicoes(declaradas);
            transicoes = Map.copyOf(declaradas);
        }
        return transicoes;
    }

    /** Situacao final: nenhuma acao vale mais. Sobrescrito por quem tem acoes. */
    protected void declararTransicoes(Map<Acao, Transicao> transicoes) {
    }
}
