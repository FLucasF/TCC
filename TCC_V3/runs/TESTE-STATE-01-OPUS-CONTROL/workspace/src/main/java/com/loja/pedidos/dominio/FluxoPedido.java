package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * As regras do processo: para cada situacao, quais acoes valem e o que cada uma faz.
 *
 * <p>Quando a logistica pedir uma etapa nova, basta acrescentar a situacao em
 * {@link Situacao} e a sua linha nesta tabela; nada mais precisa mudar.
 */
public final class FluxoPedido {

    /** Taxa cobrada quando o cancelamento acontece com o pedido ja em separacao. */
    public static final BigDecimal TAXA_DE_SEPARACAO = new BigDecimal("15.00");

    private static final Efeito REEMBOLSAR_NADA =
            pedido -> pedido.reembolsar(Dinheiro.ZERO);

    private static final Efeito REEMBOLSAR_VALOR_TOTAL =
            pedido -> pedido.reembolsar(pedido.getValorTotal());

    private static final Efeito REEMBOLSAR_VALOR_TOTAL_MENOS_TAXA_DE_SEPARACAO =
            pedido -> pedido.reembolsar(
                    Dinheiro.subtrairSemNegativo(pedido.getValorTotal(), TAXA_DE_SEPARACAO));

    private static final Efeito REEMBOLSAR_SO_OS_PRODUTOS =
            pedido -> pedido.reembolsar(pedido.getValorProdutos());

    private static final Efeito DEVOLVER_ESTOQUE_AGORA = Pedido::devolverEstoque;

    private static final Efeito AGENDAR_COLETA = Pedido::agendarColeta;

    private static final Map<Situacao, Map<Acao, Transicao>> REGRAS = Map.of(

            // O cliente ainda nao pagou, entao nao ha nada para devolver a ele.
            Situacao.AGUARDANDO_PAGAMENTO, Map.of(
                    Acao.PAGAR, new Transicao(Situacao.PAGO),
                    Acao.CANCELAR, new Transicao(Situacao.CANCELADO, REEMBOLSAR_NADA)),

            // Pagamento confirmado e nada foi separado ainda: devolve tudo.
            Situacao.PAGO, Map.of(
                    Acao.SEPARAR, new Transicao(Situacao.EM_SEPARACAO),
                    Acao.CANCELAR, new Transicao(Situacao.CANCELADO, REEMBOLSAR_VALOR_TOTAL)),

            // Cancelar aqui da mais trabalho: cobra a taxa e devolve os produtos ao estoque.
            Situacao.EM_SEPARACAO, Map.of(
                    Acao.ENVIAR, new Transicao(Situacao.ENVIADO),
                    Acao.CANCELAR, new Transicao(Situacao.CANCELADO,
                            REEMBOLSAR_VALOR_TOTAL_MENOS_TAXA_DE_SEPARACAO,
                            DEVOLVER_ESTOQUE_AGORA)),

            // Depois que saiu com a transportadora nao da mais para cancelar: so devolver depois.
            Situacao.ENVIADO, Map.of(
                    Acao.ENTREGAR, new Transicao(Situacao.ENTREGUE)),

            // O frete nao volta, e o pacote precisa ser buscado na casa do cliente.
            Situacao.ENTREGUE, Map.of(
                    Acao.DEVOLVER, new Transicao(Situacao.DEVOLVIDO,
                            REEMBOLSAR_SO_OS_PRODUTOS,
                            AGENDAR_COLETA)),

            // Acabou: nenhuma acao vale mais.
            Situacao.CANCELADO, Map.of(),
            Situacao.DEVOLVIDO, Map.of());

    private FluxoPedido() {
    }

    /** A transicao combinada para essa acao nessa situacao, ou vazio se a acao nao vale ali. */
    public static Optional<Transicao> transicao(Situacao situacao, Acao acao) {
        return Optional.ofNullable(REGRAS.getOrDefault(situacao, Map.of()).get(acao));
    }
}
