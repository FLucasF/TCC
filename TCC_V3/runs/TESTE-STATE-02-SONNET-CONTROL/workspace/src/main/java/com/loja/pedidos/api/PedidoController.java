package com.loja.pedidos.api;

import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.servico.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@RequestBody CriarPedidoRequest requisicao) {
        Pedido pedido = pedidoService.criar(requisicao.valorProdutos(), requisicao.frete());
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.de(pedido));
    }

    @PostMapping("/{id}/acoes")
    public ResponseEntity<PedidoResponse> aplicarAcao(@PathVariable String id, @RequestBody(required = false) AcaoRequest requisicao) {
        String acao = requisicao == null ? null : requisicao.acao();
        Pedido pedido = pedidoService.aplicarAcao(id, acao);
        return ResponseEntity.ok(PedidoResponse.de(pedido));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable String id) {
        Pedido pedido = pedidoService.buscar(id);
        return ResponseEntity.ok(PedidoResponse.de(pedido));
    }
}
