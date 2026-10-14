package loja.pedido;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import loja.api.PedidoRequest;
import loja.dominio.ItemPedido;
import loja.dominio.NegocioException;
import loja.dominio.Pedido;
import loja.dominio.Resumo;
import loja.dominio.Situacao;
import loja.infra.ClienteRepository;
import loja.infra.PedidoRepository;

@Service
public class PedidoService {

    private static final Duration PRAZO_PAGAMENTO = Duration.ofHours(48);

    private final Checkout checkout;
    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final List<AcaoPedido> acoes;
    private final TransactionTemplate tx;
    private final Clock clock;

    public PedidoService(Checkout checkout, PedidoRepository pedidos, ClienteRepository clientes,
            List<AcaoPedido> acoes, TransactionTemplate tx, Clock clock) {
        this.checkout = checkout;
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.acoes = acoes;
        this.tx = tx;
        this.clock = clock;
    }

    public Resumo resumo(PedidoRequest req) {
        return checkout.calcular(req);
    }

    public Pedido registrar(PedidoRequest req) {
        Resumo resumo = checkout.calcular(req);
        Pedido pedido = new Pedido(UUID.randomUUID().toString(), req.cliente().cpf(), req.cliente().email(),
                Situacao.AGUARDANDO_PAGAMENTO, Instant.now(clock).truncatedTo(ChronoUnit.SECONDS), resumo,
                req.itens().stream().map(i -> new ItemPedido(i.sku(), i.quantidade())).toList(), null, null);
        tx.executeWithoutResult(s -> {
            Object ponto = s.createSavepoint();
            try {
                clientes.criarSeNaoExiste(pedido.cpf(), pedido.email());
                s.releaseSavepoint(ponto);
            } catch (DuplicateKeyException e) { // outro pedido do mesmo CPF criou o cliente no mesmo instante
                s.rollbackToSavepoint(ponto);
            }
            pedidos.inserir(pedido);
        });
        return pedido;
    }

    public Pedido consultar(String id) {
        return pedidos.buscar(id).orElseThrow(() -> NegocioException.naoEncontrado("PEDIDO_NAO_ENCONTRADO"));
    }

    private record Resultado(Pedido pedido, NegocioException erro) {
    }

    public Pedido pagar(String id, String idTransacao, String pagoEmTexto) {
        Resultado r = tx.execute(status -> {
            Pedido p = travar(id);
            if (idTransacao == null || idTransacao.isBlank()) {
                throw NegocioException.invalido("PEDIDO_INVALIDO");
            }
            Instant pagoEm = parseData(pagoEmTexto);
            if (p.situacao() != Situacao.AGUARDANDO_PAGAMENTO) {
                if (p.situacao().pagoEAtivo() && idTransacao.equals(p.idTransacao())) {
                    return new Resultado(p, null); // aviso repetido do banco
                }
                throw NegocioException.conflito("OPERACAO_NAO_PERMITIDA");
            }
            if (Duration.between(p.criadoEm(), pagoEm).compareTo(PRAZO_PAGAMENTO) > 0) {
                cancelarEAvisar(p, false);
                return new Resultado(null, NegocioException.conflito("PEDIDO_EXPIRADO"));
            }
            Object ponto = status.createSavepoint();
            try {
                pedidos.registrarPagamento(id, idTransacao);
                acoes.forEach(a -> a.aoPagar(p));
                status.releaseSavepoint(ponto);
            } catch (NegocioException e) {
                status.rollbackToSavepoint(ponto);
                cancelarEAvisar(p, false);
                return new Resultado(null, e);
            }
            return new Resultado(pedidos.buscar(id).orElseThrow(), null);
        });
        if (r.erro() != null) {
            throw r.erro();
        }
        return r.pedido();
    }

    public Pedido enviar(String id, String codigoRastreio) {
        return tx.execute(status -> {
            Pedido p = travar(id);
            exigir(p, Situacao.ENVIADO);
            if (codigoRastreio == null || codigoRastreio.isBlank()) {
                throw NegocioException.invalido("PEDIDO_INVALIDO");
            }
            pedidos.registrarEnvio(id, codigoRastreio);
            return pedidos.buscar(id).orElseThrow();
        });
    }

    public Pedido entregar(String id) {
        return tx.execute(status -> {
            Pedido p = travar(id);
            exigir(p, Situacao.ENTREGUE);
            pedidos.atualizarSituacao(id, Situacao.ENTREGUE);
            return pedidos.buscar(id).orElseThrow();
        });
    }

    public Pedido cancelar(String id) {
        return tx.execute(status -> {
            Pedido p = travar(id);
            exigir(p, Situacao.CANCELADO);
            cancelarEAvisar(p, p.situacao() != Situacao.AGUARDANDO_PAGAMENTO);
            return pedidos.buscar(id).orElseThrow();
        });
    }

    private void cancelarEAvisar(Pedido p, boolean estavaPago) {
        pedidos.atualizarSituacao(p.id(), Situacao.CANCELADO);
        acoes.forEach(a -> a.aoCancelar(p, estavaPago));
    }

    private Pedido travar(String id) {
        return pedidos.buscarParaAtualizar(id)
                .orElseThrow(() -> NegocioException.naoEncontrado("PEDIDO_NAO_ENCONTRADO"));
    }

    private static void exigir(Pedido p, Situacao destino) {
        if (!p.situacao().podeIr(destino)) {
            throw NegocioException.conflito("OPERACAO_NAO_PERMITIDA");
        }
    }

    private Instant parseData(String texto) {
        if (texto == null || texto.isBlank()) {
            return Instant.now(clock);
        }
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException e) {
            throw NegocioException.invalido("PEDIDO_INVALIDO");
        }
    }
}
