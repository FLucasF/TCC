package com.loja.pedidos.controller;

import com.google.gson.Gson;
import com.loja.pedidos.dto.AcaoRequest;
import com.loja.pedidos.dto.CriarPedidoRequest;
import com.loja.pedidos.dto.ErrorResponse;
import com.loja.pedidos.dto.PedidoResponse;
import com.loja.pedidos.exception.PedidoException;
import com.loja.pedidos.model.Pedido;
import com.loja.pedidos.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;

    private final Gson gson = new Gson();

    @PostMapping
    public ResponseEntity<Object> criarPedido(@RequestBody String requestBody) {
        try {
            CriarPedidoRequest request = gson.fromJson(requestBody, CriarPedidoRequest.class);
            Pedido pedido = pedidoService.criarPedido(request.getValorProdutos(), request.getFrete());
            return ResponseEntity.status(201).body(new PedidoResponse(pedido));
        } catch (PedidoException e) {
            return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(e.getCodigo()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obterPedido(@PathVariable String id) {
        try {
            Pedido pedido = pedidoService.obterPedido(id);
            return ResponseEntity.ok(new PedidoResponse(pedido));
        } catch (PedidoException e) {
            return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(e.getCodigo()));
        }
    }

    @PostMapping("/{id}/acoes")
    public ResponseEntity<Object> executarAcao(@PathVariable String id, @RequestBody String requestBody) {
        try {
            AcaoRequest request = gson.fromJson(requestBody, AcaoRequest.class);
            Pedido pedido = pedidoService.executarAcao(id, request.getAcao());
            return ResponseEntity.ok(new PedidoResponse(pedido));
        } catch (PedidoException e) {
            return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(e.getCodigo()));
        }
    }
}
