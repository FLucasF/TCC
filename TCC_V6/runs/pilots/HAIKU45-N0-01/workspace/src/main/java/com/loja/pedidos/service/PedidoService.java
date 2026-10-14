package com.loja.pedidos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.pedidos.dto.*;
import com.loja.pedidos.entity.Aviso;
import com.loja.pedidos.entity.Cliente;
import com.loja.pedidos.entity.ItemPedido;
import com.loja.pedidos.entity.Pedido;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.repository.AvisoRepository;
import com.loja.pedidos.repository.ClienteRepository;
import com.loja.pedidos.repository.PedidoRepository;
import com.loja.pedidos.repository.ProdutoRepository;
import com.loja.pedidos.util.ArredondamentoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final AvisoRepository avisoRepository;
    private final ProdutoRepository produtoRepository;
    private final CalculoService calculoService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RespostaPedido registrarPedido(DadosCompra dados) {
        var resumo = calculoService.calcularResumo(dados);

        var cliente = clienteRepository.findById(dados.getCliente().getCpf())
            .orElseGet(() -> criarCliente(dados));

        var pedido = Pedido.builder()
            .cpfCliente(dados.getCliente().getCpf())
            .situacao(Pedido.SituacaoPedido.AGUARDANDO_PAGAMENTO)
            .criadoEm(Instant.now())
            .resumoJson(serializarResumo(resumo))
            .build();

        pedido = pedidoRepository.save(pedido);

        for (var item : dados.getItens()) {
            var itemPedido = ItemPedido.builder()
                .pedidoId(pedido.getId())
                .sku(item.getSku())
                .quantidade(item.getQuantidade())
                .build();
            pedido.getItens().add(itemPedido);
        }

        pedidoRepository.save(pedido);

        return mapearPedido(pedido, resumo);
    }

    @Transactional
    public RespostaPedido obterPedido(Long id) {
        var pedido = pedidoRepository.findById(id)
            .orElseThrow(ErroNegocio::pedidoNaoEncontrado);

        var resumo = desserializarResumo(pedido.getResumoJson());
        return mapearPedido(pedido, resumo);
    }

    @Transactional
    public RespostaPedido registrarPagamento(Long id, RequisicaoPagamento requisicao) {
        var pedido = pedidoRepository.findById(id)
            .orElseThrow(ErroNegocio::pedidoNaoEncontrado);

        if (pedido.getSituacao() != Pedido.SituacaoPedido.AGUARDANDO_PAGAMENTO) {
            throw ErroNegocio.operacaoNaoPermitida();
        }

        var agora = Instant.now();
        var diferenca = ChronoUnit.HOURS.between(pedido.getCriadoEm(), requisicao.getPagoEm());
        if (diferenca > 48) {
            pedido.setSituacao(Pedido.SituacaoPedido.CANCELADO);
            pedidoRepository.save(pedido);
            criarAviso(pedido, Aviso.TipoAviso.PEDIDO_CANCELADO);
            throw ErroNegocio.pedidoExpirado();
        }

        if (pedido.getIdTransacao() != null && pedido.getIdTransacao().equals(requisicao.getIdTransacao())) {
            var resumo = desserializarResumo(pedido.getResumoJson());
            return mapearPedido(pedido, resumo);
        }

        if (pedido.getIdTransacao() == null) {
            validarEstoque(pedido);

            pedido.setSituacao(Pedido.SituacaoPedido.PAGO);
            pedido.setIdTransacao(requisicao.getIdTransacao());
            pedido.setPagoEm(requisicao.getPagoEm());
            pedidoRepository.save(pedido);

            baixarEstoque(pedido);
            adicionarCreditoClube(pedido);
            criarAviso(pedido, Aviso.TipoAviso.PAGAMENTO_APROVADO);
        }

        var resumo = desserializarResumo(pedido.getResumoJson());
        return mapearPedido(pedido, resumo);
    }

    @Transactional
    public RespostaPedido registrarEnvio(Long id, RequisicaoEnvio requisicao) {
        var pedido = pedidoRepository.findById(id)
            .orElseThrow(ErroNegocio::pedidoNaoEncontrado);

        if (pedido.getSituacao() != Pedido.SituacaoPedido.PAGO) {
            throw ErroNegocio.operacaoNaoPermitida();
        }

        pedido.setSituacao(Pedido.SituacaoPedido.ENVIADO);
        pedido.setCodigoRastreio(requisicao.getCodigoRastreio());
        pedidoRepository.save(pedido);

        criarAviso(pedido, Aviso.TipoAviso.PEDIDO_ENVIADO);

        var resumo = desserializarResumo(pedido.getResumoJson());
        return mapearPedido(pedido, resumo);
    }

    @Transactional
    public RespostaPedido registrarEntrega(Long id) {
        var pedido = pedidoRepository.findById(id)
            .orElseThrow(ErroNegocio::pedidoNaoEncontrado);

        if (pedido.getSituacao() != Pedido.SituacaoPedido.ENVIADO) {
            throw ErroNegocio.operacaoNaoPermitida();
        }

        pedido.setSituacao(Pedido.SituacaoPedido.ENTREGUE);
        pedidoRepository.save(pedido);

        var resumo = desserializarResumo(pedido.getResumoJson());
        return mapearPedido(pedido, resumo);
    }

    @Transactional
    public RespostaPedido cancelarPedido(Long id) {
        var pedido = pedidoRepository.findById(id)
            .orElseThrow(ErroNegocio::pedidoNaoEncontrado);

        if (pedido.getSituacao() == Pedido.SituacaoPedido.ENVIADO ||
            pedido.getSituacao() == Pedido.SituacaoPedido.ENTREGUE) {
            throw ErroNegocio.operacaoNaoPermitida();
        }

        if (pedido.getSituacao() == Pedido.SituacaoPedido.PAGO) {
            devolverEstoque(pedido);
            removerCreditoClube(pedido);
        }

        pedido.setSituacao(Pedido.SituacaoPedido.CANCELADO);
        pedidoRepository.save(pedido);

        criarAviso(pedido, Aviso.TipoAviso.PEDIDO_CANCELADO);

        var resumo = desserializarResumo(pedido.getResumoJson());
        return mapearPedido(pedido, resumo);
    }

    private Cliente criarCliente(DadosCompra dados) {
        var cliente = Cliente.builder()
            .cpf(dados.getCliente().getCpf())
            .email(dados.getCliente().getEmail())
            .saldoCredito(BigDecimal.ZERO)
            .criadoEm(Instant.now())
            .build();
        return clienteRepository.save(cliente);
    }

    private void validarEstoque(Pedido pedido) {
        for (var item : pedido.getItens()) {
            var produto = produtoRepository.findById(item.getSku()).orElseThrow();
            if (produto.getEstoque() < item.getQuantidade()) {
                pedido.setSituacao(Pedido.SituacaoPedido.CANCELADO);
                pedidoRepository.save(pedido);
                criarAviso(pedido, Aviso.TipoAviso.PEDIDO_CANCELADO);
                throw ErroNegocio.estoqueInsuficiente(item.getSku());
            }
        }
    }

    private void baixarEstoque(Pedido pedido) {
        for (var item : pedido.getItens()) {
            var produto = produtoRepository.findById(item.getSku()).orElseThrow();
            produto.setEstoque(produto.getEstoque() - item.getQuantidade());
            produtoRepository.save(produto);
        }
    }

    private void devolverEstoque(Pedido pedido) {
        for (var item : pedido.getItens()) {
            var produto = produtoRepository.findById(item.getSku()).orElseThrow();
            produto.setEstoque(produto.getEstoque() + item.getQuantidade());
            produtoRepository.save(produto);
        }
    }

    private void adicionarCreditoClube(Pedido pedido) {
        var cliente = clienteRepository.findById(pedido.getCpfCliente()).orElseThrow();
        var resumo = desserializarResumo(pedido.getResumoJson());
        var novoSaldo = cliente.getSaldoCredito().add(resumo.getCreditoProximaCompra());
        cliente.setSaldoCredito(ArredondamentoUtil.arredondar(novoSaldo));
        clienteRepository.save(cliente);
    }

    private void removerCreditoClube(Pedido pedido) {
        var cliente = clienteRepository.findById(pedido.getCpfCliente()).orElseThrow();
        var resumo = desserializarResumo(pedido.getResumoJson());
        var novoSaldo = cliente.getSaldoCredito().subtract(resumo.getCreditoProximaCompra());
        if (novoSaldo.signum() < 0) {
            novoSaldo = BigDecimal.ZERO;
        }
        cliente.setSaldoCredito(ArredondamentoUtil.arredondar(novoSaldo));
        clienteRepository.save(cliente);
    }

    private void criarAviso(Pedido pedido, Aviso.TipoAviso tipo) {
        var aviso = Aviso.builder()
            .cpfCliente(pedido.getCpfCliente())
            .tipo(tipo)
            .pedidoId(pedido.getId())
            .situacao(Aviso.SituacaoAviso.NOVO)
            .criadoEm(Instant.now())
            .build();
        avisoRepository.save(aviso);
    }

    private String serializarResumo(Resumo resumo) {
        try {
            return objectMapper.writeValueAsString(resumo);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar resumo", e);
        }
    }

    private Resumo desserializarResumo(String resumoJson) {
        try {
            return objectMapper.readValue(resumoJson, Resumo.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao desserializar resumo", e);
        }
    }

    private RespostaPedido mapearPedido(Pedido pedido, Resumo resumo) {
        return RespostaPedido.builder()
            .id(pedido.getId())
            .situacao(pedido.getSituacao().name())
            .criadoEm(pedido.getCriadoEm())
            .resumo(resumo)
            .itens(pedido.getItens().stream()
                .map(i -> new RespostaPedido.Item(i.getSku(), i.getQuantidade()))
                .collect(Collectors.toList()))
            .codigoRastreio(pedido.getCodigoRastreio() != null ? pedido.getCodigoRastreio() : "")
            .build();
    }
}
