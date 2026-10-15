package com.loja.pedidos;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService servico;

    public PedidoController(PedidoService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@RequestBody(required = false) CriarPedidoRequest corpo) {
        Pedido pedido = servico.criar(
                corpo == null ? null : corpo.valorProdutos(),
                corpo == null ? null : corpo.frete());
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.de(pedido));
    }

    @PostMapping("/{id}/acoes")
    public PedidoResponse aplicarAcao(@PathVariable String id, @RequestBody(required = false) AcaoRequest corpo) {
        return PedidoResponse.de(servico.aplicarAcao(id, corpo == null ? null : corpo.acao()));
    }

    @GetMapping("/{id}")
    public PedidoResponse consultar(@PathVariable String id) {
        return PedidoResponse.de(servico.buscar(id));
    }
}
