package com.loja.pedidos.controller;

import com.loja.pedidos.dto.*;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<?> registrarPedido(@RequestBody DadosCompra dados) {
        try {
            var pedido = pedidoService.registrarPedido(dados);
            return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
        } catch (ErroNegocio e) {
            return ResponseEntity
                .status(e.getStatus())
                .body(new RespostaErro(e.getCodigo()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obterPedido(@PathVariable Long id) {
        try {
            var pedido = pedidoService.obterPedido(id);
            return ResponseEntity.ok(pedido);
        } catch (ErroNegocio e) {
            return ResponseEntity
                .status(e.getStatus())
                .body(new RespostaErro(e.getCodigo()));
        }
    }

    @PostMapping("/{id}/pagamento")
    public ResponseEntity<?> registrarPagamento(@PathVariable Long id, @RequestBody RequisicaoPagamento requisicao) {
        try {
            var pedido = pedidoService.registrarPagamento(id, requisicao);
            return ResponseEntity.ok(pedido);
        } catch (ErroNegocio e) {
            return ResponseEntity
                .status(e.getStatus())
                .body(new RespostaErro(e.getCodigo()));
        }
    }

    @PostMapping("/{id}/envio")
    public ResponseEntity<?> registrarEnvio(@PathVariable Long id, @RequestBody RequisicaoEnvio requisicao) {
        try {
            var pedido = pedidoService.registrarEnvio(id, requisicao);
            return ResponseEntity.ok(pedido);
        } catch (ErroNegocio e) {
            return ResponseEntity
                .status(e.getStatus())
                .body(new RespostaErro(e.getCodigo()));
        }
    }

    @PostMapping("/{id}/entrega")
    public ResponseEntity<?> registrarEntrega(@PathVariable Long id) {
        try {
            var pedido = pedidoService.registrarEntrega(id);
            return ResponseEntity.ok(pedido);
        } catch (ErroNegocio e) {
            return ResponseEntity
                .status(e.getStatus())
                .body(new RespostaErro(e.getCodigo()));
        }
    }

    @PostMapping("/{id}/cancelamento")
    public ResponseEntity<?> cancelarPedido(@PathVariable Long id) {
        try {
            var pedido = pedidoService.cancelarPedido(id);
            return ResponseEntity.ok(pedido);
        } catch (ErroNegocio e) {
            return ResponseEntity
                .status(e.getStatus())
                .body(new RespostaErro(e.getCodigo()));
        }
    }
}
