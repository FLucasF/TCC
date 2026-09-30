package com.loja.pedidos.controller;

import com.loja.pedidos.dto.AcaoRequest;
import com.loja.pedidos.dto.CriarPedidoRequest;
import com.loja.pedidos.dto.ErroResponse;
import com.loja.pedidos.dto.PedidoResponse;
import com.loja.pedidos.model.Acao;
import com.loja.pedidos.model.Pedido;
import com.loja.pedidos.service.PedidoService;
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
    public ResponseEntity<?> criarPedido(@RequestBody CriarPedidoRequest request) {
        BigDecimal valorProdutos = request.getValorProdutos();
        BigDecimal frete = request.getFrete();

        if (valorProdutos == null || valorProdutos.signum() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse("PEDIDO_INVALIDO"));
        }

        if (frete == null || frete.signum() < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse("PEDIDO_INVALIDO"));
        }

        Pedido pedido = pedidoService.criarPedido(valorProdutos, frete);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new PedidoResponse(pedido));
    }

    @PostMapping("/{id}/acoes")
    public ResponseEntity<?> executarAcao(
            @PathVariable String id,
            @RequestBody(required = false) AcaoRequest request) {

        Pedido pedido = pedidoService.obterPedido(id);
        if (pedido == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse("PEDIDO_NAO_ENCONTRADO"));
        }

        if (request == null || request.getAcao() == null || request.getAcao().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse("ACAO_INVALIDA"));
        }

        Acao acao;
        try {
            acao = Acao.valueOf(request.getAcao());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse("ACAO_INVALIDA"));
        }

        var situacaoAntes = pedido.getSituacao();
        pedidoService.executarAcao(id, acao);
        var situacaoDepois = pedido.getSituacao();

        if (situacaoDepois == situacaoAntes) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse("ACAO_NAO_PERMITIDA"));
        }

        return ResponseEntity.ok(new PedidoResponse(pedido));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obterPedido(@PathVariable String id) {
        Pedido pedido = pedidoService.obterPedido(id);
        if (pedido == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse("PEDIDO_NAO_ENCONTRADO"));
        }

        return ResponseEntity.ok(new PedidoResponse(pedido));
    }
}
