package br.com.loja.pedidos.pedido;

import br.com.loja.pedidos.catalogo.Estoque;
import br.com.loja.pedidos.checkout.CalculadoraCheckout;
import br.com.loja.pedidos.checkout.Checkout;
import br.com.loja.pedidos.checkout.DadosCompra;
import br.com.loja.pedidos.cliente.ClienteRepositorio;
import br.com.loja.pedidos.comum.ErroNegocio;
import br.com.loja.pedidos.pedido.eventos.PagamentoAprovado;
import br.com.loja.pedidos.pedido.eventos.PedidoCancelado;
import br.com.loja.pedidos.pedido.eventos.PedidoEnviado;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PedidoService {

    /** O pagamento só é aceito até 48 horas depois do registro do pedido (exatamente 48 horas ainda vale). */
    private static final Duration PRAZO_PAGAMENTO = Duration.ofHours(48);

    private final CalculadoraCheckout calculadora;
    private final PedidoRepositorio pedidos;
    private final ClienteRepositorio clientes;
    private final Estoque estoque;
    private final ApplicationEventPublisher eventos;
    private final TransactionTemplate transacao;
    private final Clock relogio;

    public PedidoService(CalculadoraCheckout calculadora, PedidoRepositorio pedidos, ClienteRepositorio clientes,
                         Estoque estoque, ApplicationEventPublisher eventos, TransactionTemplate transacao, Clock relogio) {
        this.calculadora = calculadora;
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.estoque = estoque;
        this.eventos = eventos;
        this.transacao = transacao;
        this.relogio = relogio;
    }

    public Pedido registrar(DadosCompra dados) {
        // A cotação de frete (que pode consultar a transportadora) acontece fora da transação.
        Checkout checkout = calculadora.calcular(dados);
        Instant agora = relogio.instant().truncatedTo(ChronoUnit.SECONDS);
        return transacao.execute(status -> {
            clientes.garantir(dados.cpf(), dados.email());
            long id = pedidos.inserir(checkout, agora);
            return pedidos.buscar(id).orElseThrow();
        });
    }

    public Pedido consultar(long id) {
        return pedidos.buscar(id).orElseThrow(PedidoService::naoEncontrado);
    }

    /**
     * Aviso de pagamento do banco. O mesmo aviso repetido (mesma transação) devolve o pedido como
     * está. Pagamento fora do prazo ou sem estoque cancela o pedido e é recusado.
     */
    public Pedido registrarPagamento(long id, String idTransacao, Instant pagoEm) {
        Desfecho desfecho = transacao.execute(status -> {
            Pedido pedido = pedidos.travar(id).orElseThrow(PedidoService::naoEncontrado);
            if (idTransacao == null || pagoEm == null) {
                throw requisicaoInvalida();
            }
            if (idTransacao.equals(pedido.idTransacao())) {
                return new Desfecho(pedido, null);
            }
            exigirMudanca(pedido, SituacaoPedido.PAGO);
            if (Duration.between(pedido.criadoEm(), pagoEm).compareTo(PRAZO_PAGAMENTO) > 0) {
                return new Desfecho(cancelarTravado(pedido), ErroNegocio.conflito("PEDIDO_EXPIRADO"));
            }
            if (!estoque.baixar(pedido.quantidades())) {
                return new Desfecho(cancelarTravado(pedido), ErroNegocio.conflito("ESTOQUE_INSUFICIENTE"));
            }
            pedidos.registrarPagamento(id, idTransacao, pagoEm);
            Pedido pago = pedidos.buscar(id).orElseThrow();
            eventos.publishEvent(new PagamentoAprovado(pago));
            return new Desfecho(pago, null);
        });
        // A recusa só é devolvida depois de gravado o cancelamento.
        if (desfecho.recusa() != null) {
            throw desfecho.recusa();
        }
        return desfecho.pedido();
    }

    public Pedido enviar(long id, String codigoRastreio) {
        return alterar(id, pedido -> {
            exigirMudanca(pedido, SituacaoPedido.ENVIADO);
            if (codigoRastreio == null) {
                throw requisicaoInvalida();
            }
            pedidos.registrarEnvio(id, codigoRastreio);
            Pedido enviado = pedidos.buscar(id).orElseThrow();
            eventos.publishEvent(new PedidoEnviado(enviado));
            return enviado;
        });
    }

    public Pedido entregar(long id) {
        return alterar(id, pedido -> {
            exigirMudanca(pedido, SituacaoPedido.ENTREGUE);
            pedidos.mudarSituacao(id, SituacaoPedido.ENTREGUE);
            return pedidos.buscar(id).orElseThrow();
        });
    }

    public Pedido cancelar(long id) {
        return alterar(id, pedido -> {
            exigirMudanca(pedido, SituacaoPedido.CANCELADO);
            return cancelarTravado(pedido);
        });
    }

    /** Cancela um pedido já travado, desfazendo o que o pagamento tiver feito. */
    private Pedido cancelarTravado(Pedido pedido) {
        boolean estornar = pedido.situacao().pagamentoConfirmado();
        pedidos.mudarSituacao(pedido.id(), SituacaoPedido.CANCELADO);
        if (estornar) {
            estoque.devolver(pedido.quantidades());
        }
        Pedido cancelado = pedidos.buscar(pedido.id()).orElseThrow();
        eventos.publishEvent(new PedidoCancelado(cancelado, estornar));
        return cancelado;
    }

    private Pedido alterar(long id, Function<Pedido, Pedido> operacao) {
        return transacao.execute(status -> operacao.apply(pedidos.travar(id).orElseThrow(PedidoService::naoEncontrado)));
    }

    private static void exigirMudanca(Pedido pedido, SituacaoPedido destino) {
        if (!pedido.situacao().podeIrPara(destino)) {
            throw ErroNegocio.conflito("OPERACAO_NAO_PERMITIDA");
        }
    }

    private static ErroNegocio naoEncontrado() {
        return ErroNegocio.naoEncontrado("PEDIDO_NAO_ENCONTRADO");
    }

    private static ErroNegocio requisicaoInvalida() {
        return ErroNegocio.naoProcessavel("REQUISICAO_INVALIDA");
    }

    private record Desfecho(Pedido pedido, ErroNegocio recusa) {
    }
}
