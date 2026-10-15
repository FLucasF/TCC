package com.loja.dominio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private static final Duration PRAZO_PAGAMENTO = Duration.ofHours(48);

    private final Armazem armazem;
    private final CalculadoraResumo calculadora;
    private final List<CanalNotificacao> canais;
    private final List<EfeitoPagamento> efeitos;
    private final Clock relogio;

    public PedidoService(Armazem armazem, CalculadoraResumo calculadora, List<CanalNotificacao> canais,
                         List<EfeitoPagamento> efeitos, Clock relogio) {
        this.armazem = armazem;
        this.calculadora = calculadora;
        this.canais = canais;
        this.efeitos = efeitos;
        this.relogio = relogio;
    }

    public Resumo resumir(DadosCompra dados) {
        return calculadora.calcular(dados).resumo();
    }

    public Pedido registrar(DadosCompra dados) {
        CalculadoraResumo.Calculo calculo = calculadora.calcular(dados);
        return armazem.escrever((estado, mudancas) -> {
            String cpf = dados.cliente().cpf();
            if (!estado.clientes.containsKey(cpf)) {
                Cliente cliente = new Cliente(cpf, dados.cliente().email(), Dinheiro.ZERO);
                estado.clientes.put(cpf, cliente);
                mudancas.clientes.add(cliente);
            }
            Pedido pedido = new Pedido(UUID.randomUUID().toString(), cpf, SituacaoPedido.AGUARDANDO_PAGAMENTO,
                    Instant.now(relogio).truncatedTo(ChronoUnit.SECONDS), calculo.resumo(), calculo.itens(),
                    null, null);
            estado.adicionarPedido(pedido);
            mudancas.pedidos.add(pedido);
            return pedido.copia();
        });
    }

    public Pedido consultar(String id) {
        return armazem.ler(estado -> buscar(estado, id).copia());
    }

    public Pedido pagar(String id, String idTransacao, String pagoEmTexto) {
        return armazem.escrever((estado, mudancas) -> {
            Pedido pedido = buscar(estado, id);
            if (idTransacao != null && idTransacao.equals(pedido.idTransacao())
                    && pedido.situacao() != SituacaoPedido.AGUARDANDO_PAGAMENTO) {
                return pedido.copia(); // aviso repetido do banco
            }
            if (!pedido.situacao().permite(SituacaoPedido.PAGO)) {
                throw ErroNegocio.conflito("OPERACAO_NAO_PERMITIDA");
            }
            if (idTransacao == null || idTransacao.isBlank()) {
                throw ErroNegocio.invalido("PEDIDO_INVALIDO");
            }
            Instant momento = momento(pagoEmTexto);
            if (Duration.between(pedido.criadoEm(), momento).compareTo(PRAZO_PAGAMENTO) > 0) {
                cancelarInterno(estado, mudancas, pedido);
                throw ErroNegocio.conflito("PEDIDO_EXPIRADO");
            }
            for (Item item : pedido.itens()) {
                if (estado.produtos.get(item.sku()).estoque() < item.quantidade()) {
                    cancelarInterno(estado, mudancas, pedido);
                    throw ErroNegocio.conflito("ESTOQUE_INSUFICIENTE");
                }
            }
            for (Item item : pedido.itens()) {
                Produto produto = estado.produtos.get(item.sku());
                produto.baixar(item.quantidade());
                mudancas.produtos.add(produto);
            }
            pedido.registrarPagamento(idTransacao);
            mudancas.pedidos.add(pedido);
            efeitos.forEach(e -> e.aplicar(estado, mudancas, pedido));
            avisar(estado, mudancas, pedido, TipoAviso.PAGAMENTO_APROVADO);
            return pedido.copia();
        });
    }

    public Pedido enviar(String id, String codigoRastreio) {
        return armazem.escrever((estado, mudancas) -> {
            Pedido pedido = buscar(estado, id);
            if (!pedido.situacao().permite(SituacaoPedido.ENVIADO)) {
                throw ErroNegocio.conflito("OPERACAO_NAO_PERMITIDA");
            }
            if (codigoRastreio == null || codigoRastreio.isBlank()) {
                throw ErroNegocio.invalido("PEDIDO_INVALIDO");
            }
            pedido.registrarEnvio(codigoRastreio);
            mudancas.pedidos.add(pedido);
            avisar(estado, mudancas, pedido, TipoAviso.PEDIDO_ENVIADO);
            return pedido.copia();
        });
    }

    public Pedido entregar(String id) {
        return armazem.escrever((estado, mudancas) -> {
            Pedido pedido = buscar(estado, id);
            pedido.mudarPara(SituacaoPedido.ENTREGUE);
            mudancas.pedidos.add(pedido);
            return pedido.copia();
        });
    }

    public Pedido cancelar(String id) {
        return armazem.escrever((estado, mudancas) -> {
            Pedido pedido = buscar(estado, id);
            if (!pedido.situacao().permite(SituacaoPedido.CANCELADO)) {
                throw ErroNegocio.conflito("OPERACAO_NAO_PERMITIDA");
            }
            cancelarInterno(estado, mudancas, pedido);
            return pedido.copia();
        });
    }

    private Instant momento(String texto) {
        if (texto == null) {
            return Instant.now(relogio);
        }
        try {
            return Instant.parse(texto);
        } catch (java.time.format.DateTimeParseException e) {
            throw ErroNegocio.invalido("PEDIDO_INVALIDO");
        }
    }

    private void cancelarInterno(Estado estado, Mudancas mudancas, Pedido pedido) {
        if (pedido.situacao() == SituacaoPedido.PAGO) {
            for (Item item : pedido.itens()) {
                Produto produto = estado.produtos.get(item.sku());
                produto.devolver(item.quantidade());
                mudancas.produtos.add(produto);
            }
            efeitos.forEach(e -> e.reverter(estado, mudancas, pedido));
        }
        pedido.mudarPara(SituacaoPedido.CANCELADO);
        mudancas.pedidos.add(pedido);
        avisar(estado, mudancas, pedido, TipoAviso.PEDIDO_CANCELADO);
    }

    private void avisar(Estado estado, Mudancas mudancas, Pedido pedido, TipoAviso tipo) {
        CanalNotificacao.Evento evento = new CanalNotificacao.Evento(pedido.cpf(), tipo, pedido.id());
        canais.forEach(c -> c.notificar(estado, mudancas, evento));
    }

    private static Pedido buscar(Estado estado, String id) {
        Pedido pedido = estado.pedidos.get(id);
        if (pedido == null) {
            throw ErroNegocio.naoEncontrado("PEDIDO_NAO_ENCONTRADO");
        }
        return pedido;
    }
}
