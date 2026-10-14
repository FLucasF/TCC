package com.loja.pedidos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.pedidos.dto.CheckoutRequestDTO;
import com.loja.pedidos.dto.ItemDTO;
import com.loja.pedidos.dto.ResumoDTO;
import com.loja.pedidos.domain.*;
import com.loja.pedidos.exception.PedidoException;
import com.loja.pedidos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PedidoService {
    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private AvisoRepository avisoRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private ResumoPedidoService resumoPedidoService;

    @Autowired
    private ValidacaoService validacaoService;

    @Autowired
    private ClienteService clienteService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Pedido criarPedido(CheckoutRequestDTO req) {
        // Validar tudo
        validacaoService.validarCheckout(req);

        // Calcular peso e validar disponibilidade de entrega
        BigDecimal pesoTotal = calcularPesoTotal(req.getItens());
        validacaoService.validarDisponibilidadeEntrega(req, pesoTotal);

        // Validar cupom aplicável
        validacaoService.validarCupomAplicavel(req.getCupom(), req);

        // Calcular resumo
        ResumoDTO resumo = resumoPedidoService.calcularResumo(req);

        // Validar forma de pagamento com o total final
        validacaoService.validarFormaPagamentoDisponivel(req.getFormaPagamento(), resumo.getTotalFinal());

        // Criar pedido
        List<PedidoItem> itens = req.getItens().stream()
            .map(item -> new PedidoItem(item.getSku(), item.getQuantidade()))
            .collect(Collectors.toList());

        Pedido pedido = new Pedido(req.getCliente().getCpf(), objectMapper.valueToTree(resumo), itens);

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // Criar ou atualizar cliente
        clienteService.criarOuAtualizarCliente(req.getCliente().getCpf(), req.getCliente().getEmail(), req.getNivelClube());

        return pedidoSalvo;
    }

    public Pedido obterPedido(Long id) {
        return pedidoRepository.findById(id)
            .orElseThrow(() -> new PedidoException("PEDIDO_NAO_ENCONTRADO", "Order not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Pedido processarPagamento(Long pedidoId, String idTransacao, Instant pagoEm) {
        Pedido pedido = obterPedido(pedidoId);

        // Verificar se este pagamento já foi processado
        Optional<Pagamento> pagamentoExistente = pagamentoRepository.findByIdTransacao(idTransacao);
        if (pagamentoExistente.isPresent()) {
            // Mesmo pagamento recebido novamente, não fazer nada
            return pedido;
        }

        // Verificar se o pedido está aguardando pagamento
        if (!"AGUARDANDO_PAGAMENTO".equals(pedido.getSituacao())) {
            throw new PedidoException("OPERACAO_NAO_PERMITIDA", "Payment already processed", HttpStatus.CONFLICT);
        }

        // Verificar se não passou 48 horas
        Instant agora = Instant.now();
        long diffSegundos = java.time.temporal.ChronoUnit.SECONDS.between(pedido.getCriadoEm(), pagoEm);
        if (diffSegundos > 48 * 3600) {
            // Cancelar pedido
            pedido.setSituacao("CANCELADO");
            pedidoRepository.save(pedido);
            avisoRepository.save(new Aviso(pedido.getCpfCliente(), "PEDIDO_CANCELADO", pedido.getId()));
            throw new PedidoException("PEDIDO_EXPIRADO", "Payment expired", HttpStatus.CONFLICT);
        }

        // Verificar se há estoque suficiente
        verificarEstoque(pedido);

        // Baixar estoque
        for (PedidoItem item : pedido.getItens()) {
            Produto produto = produtoRepository.findById(item.getSku()).orElseThrow();
            produto.setEstoque(produto.getEstoque() - item.getQuantidade());
            produtoRepository.save(produto);
        }

        // Adicionar crédito do clube
        BigDecimal creditoProximaCompra = extrairCreditoDoResumo(pedido);
        Optional<Cliente> cliente = clienteRepository.findByCpf(pedido.getCpfCliente());
        if (cliente.isPresent()) {
            Cliente c = cliente.get();
            c.setSaldoCredito(c.getSaldoCredito().add(creditoProximaCompra));
            clienteRepository.save(c);
        }

        // Registrar pagamento
        Pagamento pagamento = new Pagamento(pedido.getId(), idTransacao, pagoEm);
        pagamentoRepository.save(pagamento);

        // Atualizar situação do pedido
        pedido.setSituacao("PAGO");
        pedido.setIdTransacao(idTransacao);
        Pedido pedidoAtualizado = pedidoRepository.save(pedido);

        // Criar aviso
        avisoRepository.save(new Aviso(pedido.getCpfCliente(), "PAGAMENTO_APROVADO", pedido.getId()));

        return pedidoAtualizado;
    }

    @Transactional
    public Pedido enviarPedido(Long pedidoId, String codigoRastreio) {
        Pedido pedido = obterPedido(pedidoId);

        if (!"PAGO".equals(pedido.getSituacao())) {
            throw new PedidoException("OPERACAO_NAO_PERMITIDA", "Order must be paid", HttpStatus.CONFLICT);
        }

        pedido.setCodigoRastreio(codigoRastreio);
        pedido.setSituacao("ENVIADO");

        Pedido pedidoAtualizado = pedidoRepository.save(pedido);
        avisoRepository.save(new Aviso(pedido.getCpfCliente(), "PEDIDO_ENVIADO", pedido.getId()));

        return pedidoAtualizado;
    }

    @Transactional
    public Pedido entregarPedido(Long pedidoId) {
        Pedido pedido = obterPedido(pedidoId);

        if (!"ENVIADO".equals(pedido.getSituacao())) {
            throw new PedidoException("OPERACAO_NAO_PERMITIDA", "Order must be sent", HttpStatus.CONFLICT);
        }

        pedido.setSituacao("ENTREGUE");
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido cancelarPedido(Long pedidoId) {
        Pedido pedido = obterPedido(pedidoId);

        if ("ENTREGUE".equals(pedido.getSituacao()) || "ENVIADO".equals(pedido.getSituacao())) {
            throw new PedidoException("OPERACAO_NAO_PERMITIDA", "Cannot cancel sent/delivered order", HttpStatus.CONFLICT);
        }

        // Se o pedido foi pago, devolver estoque e crédito
        if ("PAGO".equals(pedido.getSituacao())) {
            // Devolver estoque
            for (PedidoItem item : pedido.getItens()) {
                Produto produto = produtoRepository.findById(item.getSku()).orElseThrow();
                produto.setEstoque(produto.getEstoque() + item.getQuantidade());
                produtoRepository.save(produto);
            }

            // Remover crédito
            BigDecimal creditoProximaCompra = extrairCreditoDoResumo(pedido);
            Optional<Cliente> cliente = clienteRepository.findByCpf(pedido.getCpfCliente());
            if (cliente.isPresent()) {
                Cliente c = cliente.get();
                c.setSaldoCredito(c.getSaldoCredito().subtract(creditoProximaCompra));
                if (c.getSaldoCredito().compareTo(BigDecimal.ZERO) < 0) {
                    c.setSaldoCredito(BigDecimal.ZERO);
                }
                clienteRepository.save(c);
            }
        }

        pedido.setSituacao("CANCELADO");
        Pedido pedidoAtualizado = pedidoRepository.save(pedido);

        avisoRepository.save(new Aviso(pedido.getCpfCliente(), "PEDIDO_CANCELADO", pedido.getId()));

        return pedidoAtualizado;
    }

    private void verificarEstoque(Pedido pedido) {
        for (PedidoItem item : pedido.getItens()) {
            Produto produto = produtoRepository.findById(item.getSku()).orElseThrow();
            if (produto.getEstoque() < item.getQuantidade()) {
                pedido.setSituacao("CANCELADO");
                pedidoRepository.save(pedido);
                avisoRepository.save(new Aviso(pedido.getCpfCliente(), "PEDIDO_CANCELADO", pedido.getId()));
                throw new PedidoException("ESTOQUE_INSUFICIENTE", "Insufficient stock", HttpStatus.CONFLICT);
            }
        }
    }

    private BigDecimal calcularPesoTotal(java.util.List<ItemDTO> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemDTO item : itens) {
            Optional<Produto> produto = produtoRepository.findById(item.getSku());
            if (produto.isPresent()) {
                BigDecimal pesoUnitario = produto.get().getPesoKg();
                BigDecimal quantidade = new BigDecimal(item.getQuantidade());
                peso = peso.add(pesoUnitario.multiply(quantidade));
            }
        }
        return peso;
    }

    private BigDecimal extrairCreditoDoResumo(Pedido pedido) {
        try {
            return new BigDecimal(pedido.getResumo().get("creditoProximaCompra").asText());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
