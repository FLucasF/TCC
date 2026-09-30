package com.loja;

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
        if (request.getValorProdutos() == null || request.getValorProdutos().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(new ErroResponse("PEDIDO_INVALIDO"));
        }
        if (request.getFrete() == null || request.getFrete().compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().body(new ErroResponse("PEDIDO_INVALIDO"));
        }

        Pedido pedido = pedidoService.criarPedido(request.getValorProdutos(), request.getFrete());
        return ResponseEntity.status(HttpStatus.CREATED).body(new PedidoResponse(pedido));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obterPedido(@PathVariable String id) {
        Pedido pedido = pedidoService.obterPedido(id);
        if (pedido == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse("PEDIDO_NAO_ENCONTRADO"));
        }
        return ResponseEntity.ok(new PedidoResponse(pedido));
    }

    @PostMapping("/{id}/acoes")
    public ResponseEntity<?> executarAcao(@PathVariable String id, @RequestBody AcaoRequest request) {
        Pedido pedido = pedidoService.obterPedido(id);
        if (pedido == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse("PEDIDO_NAO_ENCONTRADO"));
        }

        if (request.getAcao() == null || request.getAcao().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErroResponse("ACAO_INVALIDA"));
        }

        Acao acao;
        try {
            acao = Acao.valueOf(request.getAcao());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("ACAO_INVALIDA"));
        }

        Situacao situacaoAnterior = pedido.getSituacao();
        pedidoService.executarAcao(id, acao);

        if (pedido.getSituacao() == situacaoAnterior) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse("ACAO_NAO_PERMITIDA"));
        }

        return ResponseEntity.ok(new PedidoResponse(pedido));
    }
}
