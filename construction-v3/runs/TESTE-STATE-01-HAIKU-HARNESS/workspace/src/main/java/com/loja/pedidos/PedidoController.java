package com.loja.pedidos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody CriarPedidoRequest request) {
        if (request.getValorProdutos() == null || request.getValorProdutos().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(400).body(new ErrorResponse("PEDIDO_INVALIDO"));
        }

        if (request.getFrete() == null || request.getFrete().compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.status(400).body(new ErrorResponse("PEDIDO_INVALIDO"));
        }

        Pedido pedido = pedidoService.criar(request.getValorProdutos(), request.getFrete());
        return ResponseEntity.status(201).body(pedido);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obter(@PathVariable String id) {
        Pedido pedido = pedidoService.obter(id);
        if (pedido == null) {
            return ResponseEntity.status(404).body(new ErrorResponse("PEDIDO_NAO_ENCONTRADO"));
        }
        return ResponseEntity.ok(pedido);
    }

    @PostMapping("/{id}/acoes")
    public ResponseEntity<?> executarAcao(@PathVariable String id, @RequestBody AcaoRequest request) {
        Pedido pedido = pedidoService.obter(id);
        if (pedido == null) {
            return ResponseEntity.status(404).body(new ErrorResponse("PEDIDO_NAO_ENCONTRADO"));
        }

        if (request.getAcao() == null || request.getAcao().isEmpty()) {
            return ResponseEntity.status(400).body(new ErrorResponse("ACAO_INVALIDA"));
        }

        Acao acao;
        try {
            acao = Acao.valueOf(request.getAcao());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(new ErrorResponse("ACAO_INVALIDA"));
        }

        Pedido pedidoAtualizado = pedidoService.executarAcao(id, acao);
        if (pedidoAtualizado == null) {
            return ResponseEntity.status(409).body(new ErrorResponse("ACAO_NAO_PERMITIDA"));
        }

        return ResponseEntity.ok(pedidoAtualizado);
    }
}
