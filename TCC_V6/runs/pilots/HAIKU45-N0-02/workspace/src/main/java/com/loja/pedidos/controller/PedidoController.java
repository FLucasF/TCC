package com.loja.pedidos.controller;

import com.loja.pedidos.domain.Pedido;
import com.loja.pedidos.domain.PedidoItem;
import com.loja.pedidos.dto.CheckoutRequestDTO;
import com.loja.pedidos.dto.ItemDTO;
import com.loja.pedidos.dto.PedidoResponseDTO;
import com.loja.pedidos.service.PedidoService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criarPedido(@RequestBody CheckoutRequestDTO req) {
        Pedido pedido = pedidoService.criarPedido(req);
        PedidoResponseDTO response = converterParaResponse(pedido);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> obterPedido(@PathVariable Long id) {
        Pedido pedido = pedidoService.obterPedido(id);
        PedidoResponseDTO response = converterParaResponse(pedido);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pagamento")
    public ResponseEntity<PedidoResponseDTO> registrarPagamento(
            @PathVariable Long id,
            @RequestBody PagamentoRequestDTO req) {
        Instant pagoEm = Instant.parse(req.pagoEm);
        Pedido pedido = pedidoService.processarPagamento(id, req.idTransacao, pagoEm);
        PedidoResponseDTO response = converterParaResponse(pedido);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/envio")
    public ResponseEntity<PedidoResponseDTO> enviarPedido(
            @PathVariable Long id,
            @RequestBody EnvioRequestDTO req) {
        Pedido pedido = pedidoService.enviarPedido(id, req.codigoRastreio);
        PedidoResponseDTO response = converterParaResponse(pedido);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/entrega")
    public ResponseEntity<PedidoResponseDTO> entregarPedido(@PathVariable Long id) {
        Pedido pedido = pedidoService.entregarPedido(id);
        PedidoResponseDTO response = converterParaResponse(pedido);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancelamento")
    public ResponseEntity<PedidoResponseDTO> cancelarPedido(@PathVariable Long id) {
        Pedido pedido = pedidoService.cancelarPedido(id);
        PedidoResponseDTO response = converterParaResponse(pedido);
        return ResponseEntity.ok(response);
    }

    private PedidoResponseDTO converterParaResponse(Pedido pedido) {
        List<ItemDTO> itens = pedido.getItens().stream()
            .map(item -> new ItemDTO(item.getSku(), item.getQuantidade()))
            .collect(Collectors.toList());

        return new PedidoResponseDTO(
            pedido.getId(),
            pedido.getSituacao(),
            pedido.getCriadoEm(),
            pedido.getResumo(),
            itens,
            pedido.getCodigoRastreio()
        );
    }

    public static class PagamentoRequestDTO {
        public String idTransacao;
        public String pagoEm;

        public PagamentoRequestDTO() {}
    }

    public static class EnvioRequestDTO {
        public String codigoRastreio;

        public EnvioRequestDTO() {}
    }
}
