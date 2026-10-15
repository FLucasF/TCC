package com.loja.pedidos.api;

import com.loja.pedidos.servico.PedidoServico;
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

    private final PedidoServico servico;

    public PedidoController(PedidoServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<PedidoResposta> criar(
            @RequestBody(required = false) CriarPedidoRequisicao requisicao) {
        CriarPedidoRequisicao pedido = requisicao == null
                ? new CriarPedidoRequisicao(null, null)
                : requisicao;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PedidoResposta.de(servico.criar(pedido.valorProdutos(), pedido.frete())));
    }

    @PostMapping("/{id}/acoes")
    public PedidoResposta aplicarAcao(
            @PathVariable String id,
            @RequestBody(required = false) AcaoRequisicao requisicao) {
        return PedidoResposta.de(servico.aplicarAcao(id, requisicao == null ? null : requisicao.acao()));
    }

    @GetMapping("/{id}")
    public PedidoResposta consultar(@PathVariable String id) {
        return PedidoResposta.de(servico.buscar(id));
    }
}
