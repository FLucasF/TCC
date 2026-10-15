package com.loja.pedidos.api;

import com.loja.pedidos.dominio.Acao;
import com.loja.pedidos.dominio.Pedido;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidosEmMemoria pedidos;

    public PedidoController(PedidosEmMemoria pedidos) {
        this.pedidos = pedidos;
    }

    @PostMapping
    public ResponseEntity<PedidoResposta> criar(@RequestBody(required = false) NovoPedido novoPedido) {
        if (novoPedido == null || invalido(novoPedido)) {
            throw Erro.PEDIDO_INVALIDO.excecao();
        }
        Pedido pedido = pedidos.criar(novoPedido.valorProdutos(), novoPedido.frete());
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResposta.de(pedido));
    }

    @GetMapping("/{id}")
    public PedidoResposta consultar(@PathVariable String id) {
        return PedidoResposta.de(buscar(id));
    }

    @PostMapping("/{id}/acoes")
    public PedidoResposta agir(@PathVariable String id, @RequestBody(required = false) AcaoPedida acaoPedida) {
        Pedido pedido = buscar(id);
        Acao acao = Acao.deNome(acaoPedida == null ? null : acaoPedida.acao())
                .orElseThrow(Erro.ACAO_INVALIDA::excecao);
        if (!pedido.aplicar(acao)) {
            throw Erro.ACAO_NAO_PERMITIDA.excecao();
        }
        return PedidoResposta.de(pedido);
    }

    private Pedido buscar(String id) {
        return pedidos.buscar(id).orElseThrow(Erro.PEDIDO_NAO_ENCONTRADO::excecao);
    }

    private boolean invalido(NovoPedido novoPedido) {
        return novoPedido.valorProdutos() == null
                || novoPedido.valorProdutos().compareTo(BigDecimal.ZERO) <= 0
                || novoPedido.frete() == null
                || novoPedido.frete().compareTo(BigDecimal.ZERO) < 0;
    }
}
