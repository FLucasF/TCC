package com.loja.pedidos.checkout;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loja.pedidos.acoes.AcaoCancelamento;
import com.loja.pedidos.acoes.AcaoPagamentoConfirmado;
import com.loja.pedidos.acoes.Notificador;
import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.Resumo;
import com.loja.pedidos.dominio.SituacaoPedido;
import com.loja.pedidos.dominio.TipoAviso;
import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;
import com.loja.pedidos.persistencia.ClienteRepositorio;
import com.loja.pedidos.persistencia.PedidoRepositorio;
import com.loja.pedidos.persistencia.ProdutoRepositorio;

/**
 * Ciclo de vida do pedido. Os métodos de escrita travam a linha do pedido, então operações simultâneas
 * sobre o mesmo pedido (ex.: aviso de pagamento repetido) são executadas uma de cada vez.
 * Erros de negócio não desfazem a transação: um pagamento expirado ou sem estoque precisa gravar o cancelamento.
 */
@Service
@Transactional(noRollbackFor = ErroNegocio.class)
public class PedidoServico {

    private static final Duration PRAZO_PAGAMENTO = Duration.ofHours(48);

    private final CalculadoraResumo calculadora;
    private final PedidoRepositorio pedidos;
    private final ClienteRepositorio clientes;
    private final ProdutoRepositorio produtos;
    private final List<AcaoPagamentoConfirmado> aoPagar;
    private final List<AcaoCancelamento> aoCancelar;
    private final List<Notificador> notificadores;
    private final Clock relogio;

    public PedidoServico(CalculadoraResumo calculadora, PedidoRepositorio pedidos, ClienteRepositorio clientes,
            ProdutoRepositorio produtos, List<AcaoPagamentoConfirmado> aoPagar, List<AcaoCancelamento> aoCancelar,
            List<Notificador> notificadores, Clock relogio) {
        this.calculadora = calculadora;
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.produtos = produtos;
        this.aoPagar = aoPagar;
        this.aoCancelar = aoCancelar;
        this.notificadores = notificadores;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Resumo resumo(CompraRequest compra) {
        return calculadora.calcular(compra);
    }

    public Pedido registrar(CompraRequest compra) {
        Resumo resumo = calculadora.calcular(compra);
        clientes.criarSeAusente(compra.cliente().cpf(), compra.cliente().email());
        Pedido pedido = new Pedido(
                UUID.randomUUID().toString(),
                compra.cliente().cpf(),
                compra.cliente().email(),
                compra.cep(),
                compra.regiao(),
                compra.modalidadeEntrega(),
                compra.cupom(),
                compra.formaPagamento(),
                compra.nivelClube(),
                SituacaoPedido.AGUARDANDO_PAGAMENTO,
                relogio.instant().truncatedTo(ChronoUnit.SECONDS),
                resumo,
                CalculadoraResumo.itensDe(compra),
                null,
                null);
        pedidos.inserir(pedido);
        return pedido;
    }

    @Transactional(readOnly = true)
    public Pedido consultar(String id) {
        return pedidos.buscar(id).orElseThrow(() -> new ErroNegocio(CodigoErro.PEDIDO_NAO_ENCONTRADO));
    }

    public Pedido pagar(String id, String idTransacao, Instant pagoEm) {
        Pedido pedido = travar(id);
        if (idTransacao == null || idTransacao.isBlank()) {
            throw new ErroNegocio(CodigoErro.PEDIDO_INVALIDO);
        }
        if (pedido.idTransacao() != null && pedido.idTransacao().equals(idTransacao)
                && pedido.situacao() != SituacaoPedido.CANCELADO) {
            return pedido; // aviso repetido: nada a fazer
        }
        if (pedido.situacao() != SituacaoPedido.AGUARDANDO_PAGAMENTO) {
            throw new ErroNegocio(CodigoErro.OPERACAO_NAO_PERMITIDA);
        }
        Instant momento = pagoEm != null ? pagoEm : relogio.instant();
        if (momento.isAfter(pedido.criadoEm().plus(PRAZO_PAGAMENTO))) {
            cancelar(pedido, false);
            throw new ErroNegocio(CodigoErro.PEDIDO_EXPIRADO);
        }
        if (!produtos.baixarEstoque(pedido.itens())) {
            cancelar(pedido, false);
            throw new ErroNegocio(CodigoErro.ESTOQUE_INSUFICIENTE_PAGAMENTO);
        }
        pedidos.registrarPagamento(id, idTransacao);
        Pedido pago = pedidos.buscar(id).orElseThrow();
        aoPagar.forEach(a -> a.executar(pago));
        return pago;
    }

    public Pedido enviar(String id, String codigoRastreio) {
        Pedido pedido = travar(id);
        if (pedido.situacao() != SituacaoPedido.PAGO) {
            throw new ErroNegocio(CodigoErro.OPERACAO_NAO_PERMITIDA);
        }
        if (codigoRastreio == null || codigoRastreio.isBlank()) {
            throw new ErroNegocio(CodigoErro.PEDIDO_INVALIDO);
        }
        pedidos.registrarEnvio(id, codigoRastreio);
        Pedido enviado = pedidos.buscar(id).orElseThrow();
        notificadores.forEach(n -> n.notificar(TipoAviso.PEDIDO_ENVIADO, enviado));
        return enviado;
    }

    public Pedido entregar(String id) {
        Pedido pedido = travar(id);
        if (pedido.situacao() != SituacaoPedido.ENVIADO) {
            throw new ErroNegocio(CodigoErro.OPERACAO_NAO_PERMITIDA);
        }
        pedidos.atualizarSituacao(id, SituacaoPedido.ENTREGUE);
        return pedidos.buscar(id).orElseThrow();
    }

    public Pedido cancelar(String id) {
        Pedido pedido = travar(id);
        if (pedido.situacao() != SituacaoPedido.AGUARDANDO_PAGAMENTO && pedido.situacao() != SituacaoPedido.PAGO) {
            throw new ErroNegocio(CodigoErro.OPERACAO_NAO_PERMITIDA);
        }
        cancelar(pedido, pedido.situacao() == SituacaoPedido.PAGO);
        return pedidos.buscar(id).orElseThrow();
    }

    private void cancelar(Pedido pedido, boolean estavaPago) {
        pedidos.atualizarSituacao(pedido.id(), SituacaoPedido.CANCELADO);
        aoCancelar.forEach(a -> a.executar(pedido, estavaPago));
    }

    private Pedido travar(String id) {
        return pedidos.buscarComTrava(id).orElseThrow(() -> new ErroNegocio(CodigoErro.PEDIDO_NAO_ENCONTRADO));
    }
}
