package br.com.loja.pedidos.pedido;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.loja.pedidos.aviso.AvisoRepositorio;
import br.com.loja.pedidos.aviso.TipoAviso;
import br.com.loja.pedidos.catalogo.Produto;
import br.com.loja.pedidos.catalogo.ProdutoRepositorio;
import br.com.loja.pedidos.checkout.CalculadoraResumo;
import br.com.loja.pedidos.checkout.Checkout;
import br.com.loja.pedidos.cliente.ClienteRepositorio;
import br.com.loja.pedidos.comum.ErroNegocio;
import br.com.loja.pedidos.comum.Identificador;
import tools.jackson.databind.JsonNode;

@Service
public class PedidoService {

    private static final Duration PRAZO_PAGAMENTO = Duration.ofHours(48);

    private final CalculadoraResumo calculadora;
    private final PedidoRepositorio pedidos;
    private final ProdutoRepositorio produtos;
    private final ClienteRepositorio clientes;
    private final AvisoRepositorio avisos;
    private final List<EfeitoPagamento> efeitosPagamento;
    private final Clock relogio;
    private final TransactionTemplate transacao;

    public PedidoService(CalculadoraResumo calculadora, PedidoRepositorio pedidos, ProdutoRepositorio produtos,
            ClienteRepositorio clientes, AvisoRepositorio avisos, List<EfeitoPagamento> efeitosPagamento,
            Clock relogio, PlatformTransactionManager transacoes) {
        this.calculadora = calculadora;
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.clientes = clientes;
        this.avisos = avisos;
        this.efeitosPagamento = efeitosPagamento;
        this.relogio = relogio;
        this.transacao = new TransactionTemplate(transacoes);
    }

    public Pedido registrar(JsonNode dados) {
        // O cálculo (que pode consultar a transportadora) fica fora da transação.
        Checkout checkout = calculadora.calcular(dados);
        return transacao.execute(status -> gravar(checkout));
    }

    private Pedido gravar(Checkout checkout) {
        clientes.garantir(checkout.cpf(), checkout.email());
        List<ItemPedido> itens = checkout.itens().stream()
                .map(item -> new ItemPedido(item.produto().sku(), item.quantidade())).toList();
        long id = pedidos.inserir(checkout.cpf(), Instant.now(relogio).truncatedTo(ChronoUnit.SECONDS),
                checkout.resumo(), itens);
        return pedidos.buscar(id).orElseThrow();
    }

    public Pedido consultar(String id) {
        return pedidos.buscar(lerId(id)).orElseThrow(PedidoService::naoEncontrado);
    }

    /**
     * Confirma o pagamento. O pedido fica travado durante a operação, então avisos repetidos ou simultâneos do banco
     * são tratados um de cada vez. Recusas que mudam o pedido (expiração, falta de estoque) são gravadas mesmo assim.
     */
    @Transactional(noRollbackFor = ErroNegocio.class)
    public Pedido pagar(String id, String idTransacao, Instant pagoEm) {
        Pedido pedido = travar(id);
        if (idTransacao != null && idTransacao.equals(pedido.idTransacao())) {
            return pedido;
        }
        if (!pedido.situacao().podeIrPara(SituacaoPedido.PAGO)) {
            throw ErroNegocio.conflito("OPERACAO_NAO_PERMITIDA");
        }
        if (idTransacao == null || idTransacao.isBlank() || pagoEm == null) {
            throw ErroNegocio.invalido("PEDIDO_INVALIDO");
        }
        if (pagoEm.isAfter(pedido.criadoEm().plus(PRAZO_PAGAMENTO))) {
            cancelarSemPagamento(pedido);
            throw ErroNegocio.conflito("PEDIDO_EXPIRADO");
        }
        if (!baixarEstoque(pedido)) {
            cancelarSemPagamento(pedido);
            throw ErroNegocio.conflito("ESTOQUE_INSUFICIENTE");
        }
        pedidos.registrarPagamento(pedido.id(), idTransacao, pagoEm);
        Pedido pago = pedidos.buscar(pedido.id()).orElseThrow();
        efeitosPagamento.forEach(efeito -> efeito.aoConfirmar(pago));
        return pago;
    }

    @Transactional
    public Pedido enviar(String id, String codigoRastreio) {
        Pedido pedido = travar(id);
        exigirTransicao(pedido, SituacaoPedido.ENVIADO);
        if (codigoRastreio == null || codigoRastreio.isBlank()) {
            throw ErroNegocio.invalido("PEDIDO_INVALIDO");
        }
        pedidos.registrarEnvio(pedido.id(), codigoRastreio);
        avisos.registrar(pedido.cpf(), TipoAviso.PEDIDO_ENVIADO, pedido.id());
        return pedidos.buscar(pedido.id()).orElseThrow();
    }

    @Transactional
    public Pedido entregar(String id) {
        Pedido pedido = travar(id);
        exigirTransicao(pedido, SituacaoPedido.ENTREGUE);
        pedidos.alterarSituacao(pedido.id(), SituacaoPedido.ENTREGUE);
        return pedidos.buscar(pedido.id()).orElseThrow();
    }

    @Transactional
    public Pedido cancelar(String id) {
        Pedido pedido = travar(id);
        exigirTransicao(pedido, SituacaoPedido.CANCELADO);
        if (pedido.situacao().pago()) {
            produtos.travar(pedido.itens().stream().map(ItemPedido::sku).toList());
            pedido.itens().forEach(item -> produtos.alterarEstoque(item.sku(), item.quantidade()));
            efeitosPagamento.forEach(efeito -> efeito.aoCancelarPedidoPago(pedido));
        }
        pedidos.alterarSituacao(pedido.id(), SituacaoPedido.CANCELADO);
        avisos.registrar(pedido.cpf(), TipoAviso.PEDIDO_CANCELADO, pedido.id());
        return pedidos.buscar(pedido.id()).orElseThrow();
    }

    private void cancelarSemPagamento(Pedido pedido) {
        pedidos.alterarSituacao(pedido.id(), SituacaoPedido.CANCELADO);
        avisos.registrar(pedido.cpf(), TipoAviso.PEDIDO_CANCELADO, pedido.id());
    }

    /** Baixa o estoque de todos os itens, ou de nenhum se faltar algum. */
    private boolean baixarEstoque(Pedido pedido) {
        Map<String, Produto> travados = produtos.travar(pedido.itens().stream().map(ItemPedido::sku).toList()).stream()
                .collect(Collectors.toMap(Produto::sku, Function.identity()));
        boolean haEstoque = pedido.itens().stream()
                .allMatch(item -> travados.get(item.sku()).estoque() >= item.quantidade());
        if (haEstoque) {
            pedido.itens().forEach(item -> produtos.alterarEstoque(item.sku(), -item.quantidade()));
        }
        return haEstoque;
    }

    private Pedido travar(String id) {
        return pedidos.travar(lerId(id)).orElseThrow(PedidoService::naoEncontrado);
    }

    private static void exigirTransicao(Pedido pedido, SituacaoPedido destino) {
        if (!pedido.situacao().podeIrPara(destino)) {
            throw ErroNegocio.conflito("OPERACAO_NAO_PERMITIDA");
        }
    }

    private static long lerId(String id) {
        return Identificador.ler(id).orElseThrow(PedidoService::naoEncontrado);
    }

    private static ErroNegocio naoEncontrado() {
        return ErroNegocio.naoEncontrado("PEDIDO_NAO_ENCONTRADO");
    }
}
