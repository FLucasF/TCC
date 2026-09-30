package com.loja.pedidos.api;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loja.pedidos.api.dto.AcaoRequest;
import com.loja.pedidos.api.dto.CriarPedidoRequest;
import com.loja.pedidos.api.dto.PedidoResponse;
import com.loja.pedidos.dominio.Acao;
import com.loja.pedidos.dominio.AcaoInvalidaException;
import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.PedidoInvalidoException;
import com.loja.pedidos.dominio.PedidoRepositorio;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoRepositorio pedidoRepositorio;

    public PedidoController(PedidoRepositorio pedidoRepositorio) {
        this.pedidoRepositorio = pedidoRepositorio;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@RequestBody(required = false) CriarPedidoRequest request) {
        BigDecimal valorProdutos = request == null ? null : request.valorProdutos();
        BigDecimal frete = request == null ? null : request.frete();

        if (valorProdutos == null || valorProdutos.signum() <= 0 || frete == null || frete.signum() < 0) {
            throw new PedidoInvalidoException();
        }

        Pedido pedido = pedidoRepositorio.criar(valorProdutos, frete);
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.de(pedido));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable String id) {
        Pedido pedido = pedidoRepositorio.buscar(id);
        return ResponseEntity.ok(PedidoResponse.de(pedido));
    }

    @PostMapping("/{id}/acoes")
    public ResponseEntity<PedidoResponse> aplicarAcao(@PathVariable String id,
            @RequestBody(required = false) AcaoRequest request) {
        Pedido pedido = pedidoRepositorio.buscar(id);

        String nomeAcao = request == null ? null : request.acao();
        Acao acao = converterAcao(nomeAcao);

        pedido.aplicar(acao);
        return ResponseEntity.ok(PedidoResponse.de(pedido));
    }

    private Acao converterAcao(String nomeAcao) {
        if (nomeAcao == null) {
            throw new AcaoInvalidaException(null);
        }
        try {
            return Acao.valueOf(nomeAcao);
        } catch (IllegalArgumentException e) {
            throw new AcaoInvalidaException(nomeAcao);
        }
    }
}
