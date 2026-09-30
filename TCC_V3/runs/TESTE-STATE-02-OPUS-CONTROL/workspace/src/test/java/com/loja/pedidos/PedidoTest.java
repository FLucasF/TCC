package com.loja.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.pedidos.dominio.Acao;
import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.Situacao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** As regras de cada situacao, sem passar pela web. */
class PedidoTest {

    private static Pedido pedido(String produtos, String frete) {
        return new Pedido("1", new BigDecimal(produtos), new BigDecimal(frete));
    }

    private static Pedido pedido(String produtos, String frete, Acao... acoes) {
        Pedido pedido = pedido(produtos, frete);
        for (Acao acao : acoes) {
            pedido.aplicar(acao);
        }
        return pedido;
    }

    @Test
    void comecaAguardandoPagamentoSemReembolso() {
        Pedido pedido = pedido("200.00", "20.00");

        assertThat(pedido.situacao()).isEqualTo(Situacao.AGUARDANDO_PAGAMENTO);
        assertThat(pedido.valorTotal()).isEqualByComparingTo("220.00");
        assertThat(pedido.valorReembolsado()).isEqualByComparingTo("0.00");
        assertThat(pedido.estoqueDevolvido()).isFalse();
        assertThat(pedido.coletaAgendada()).isFalse();
        assertThat(pedido.historico()).containsExactly(Situacao.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void cancelarAguardandoPagamentoNaoReembolsaNada() {
        Pedido pedido = pedido("80.00", "0.00", Acao.CANCELAR);

        assertThat(pedido.situacao()).isEqualTo(Situacao.CANCELADO);
        assertThat(pedido.valorReembolsado()).isEqualByComparingTo("0.00");
        assertThat(pedido.estoqueDevolvido()).isFalse();
        assertThat(pedido.coletaAgendada()).isFalse();
    }

    @Test
    void cancelarPagoReembolsaOTotal() {
        Pedido pedido = pedido("250.00", "25.00", Acao.PAGAR, Acao.CANCELAR);

        assertThat(pedido.situacao()).isEqualTo(Situacao.CANCELADO);
        assertThat(pedido.valorReembolsado()).isEqualByComparingTo("275.00");
        assertThat(pedido.estoqueDevolvido()).isFalse();
    }

    @Test
    void cancelarEmSeparacaoCobraTaxaEDevolveEstoque() {
        Pedido pedido = pedido("200.00", "20.00", Acao.PAGAR, Acao.SEPARAR, Acao.CANCELAR);

        assertThat(pedido.situacao()).isEqualTo(Situacao.CANCELADO);
        assertThat(pedido.valorReembolsado()).isEqualByComparingTo("205.00");
        assertThat(pedido.estoqueDevolvido()).isTrue();
        assertThat(pedido.coletaAgendada()).isFalse();
        assertThat(pedido.historico()).containsExactly(
                Situacao.AGUARDANDO_PAGAMENTO, Situacao.PAGO, Situacao.EM_SEPARACAO, Situacao.CANCELADO);
    }

    @Test
    void reembolsoNaSeparacaoNuncaFicaNegativo() {
        Pedido pedido = pedido("10.00", "3.00", Acao.PAGAR, Acao.SEPARAR, Acao.CANCELAR);

        assertThat(pedido.valorTotal()).isEqualByComparingTo("13.00");
        assertThat(pedido.valorReembolsado()).isEqualByComparingTo("0.00");
        assertThat(pedido.estoqueDevolvido()).isTrue();
    }

    @Test
    void devolucaoNaoReembolsaOFreteEAgendaColeta() {
        Pedido pedido = pedido("150.00", "12.50",
                Acao.PAGAR, Acao.SEPARAR, Acao.ENVIAR, Acao.ENTREGAR, Acao.DEVOLVER);

        assertThat(pedido.situacao()).isEqualTo(Situacao.DEVOLVIDO);
        assertThat(pedido.valorTotal()).isEqualByComparingTo("162.50");
        assertThat(pedido.valorReembolsado()).isEqualByComparingTo("150.00");
        assertThat(pedido.coletaAgendada()).isTrue();
        assertThat(pedido.estoqueDevolvido()).isFalse();
    }

    @Test
    void pedidoEnviadoNaoPodeMaisSerCancelado() {
        Pedido pedido = pedido("99.90", "15.00", Acao.PAGAR, Acao.SEPARAR, Acao.ENVIAR);

        assertThat(pedido.aplicar(Acao.CANCELAR)).isEmpty();
        assertThat(pedido.situacao()).isEqualTo(Situacao.ENVIADO);
        assertThat(pedido.valorReembolsado()).isEqualByComparingTo("0.00");
    }

    @Test
    void acaoNaoPermitidaDeixaOPedidoComoEstava() {
        Pedido pedido = pedido("99.90", "15.00", Acao.PAGAR);

        assertThat(pedido.aplicar(Acao.ENVIAR)).isEmpty();
        assertThat(pedido.situacao()).isEqualTo(Situacao.PAGO);
        assertThat(pedido.historico()).containsExactly(Situacao.AGUARDANDO_PAGAMENTO, Situacao.PAGO);
    }

    @Test
    void situacoesFinaisNaoAceitamMaisNenhumaAcao() {
        Pedido cancelado = pedido("50.00", "5.00", Acao.CANCELAR);
        Pedido devolvido = pedido("50.00", "5.00",
                Acao.PAGAR, Acao.SEPARAR, Acao.ENVIAR, Acao.ENTREGAR, Acao.DEVOLVER);

        for (Acao acao : Acao.values()) {
            assertThat(cancelado.aplicar(acao)).isEmpty();
            assertThat(devolvido.aplicar(acao)).isEmpty();
        }
        assertThat(cancelado.situacao()).isEqualTo(Situacao.CANCELADO);
        assertThat(devolvido.situacao()).isEqualTo(Situacao.DEVOLVIDO);
    }

    @Test
    void cadaSituacaoTemOTextoQueOClienteVe() {
        assertThat(Situacao.AGUARDANDO_PAGAMENTO.descricao()).isEqualTo("Aguardando pagamento");
        assertThat(Situacao.PAGO.descricao()).isEqualTo("Pagamento confirmado");
        assertThat(Situacao.EM_SEPARACAO.descricao()).isEqualTo("Separando seus produtos");
        assertThat(Situacao.ENVIADO.descricao()).isEqualTo("A caminho");
        assertThat(Situacao.ENTREGUE.descricao()).isEqualTo("Entregue");
        assertThat(Situacao.CANCELADO.descricao()).isEqualTo("Cancelado");
        assertThat(Situacao.DEVOLVIDO.descricao()).isEqualTo("Devolvido");
    }
}
