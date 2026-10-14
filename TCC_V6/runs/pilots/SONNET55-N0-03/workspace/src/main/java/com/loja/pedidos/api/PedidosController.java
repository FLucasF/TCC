package com.loja.pedidos.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.loja.pedidos.checkout.CompraRequest;
import com.loja.pedidos.checkout.PedidoServico;
import com.loja.pedidos.dominio.Aviso;
import com.loja.pedidos.dominio.Cliente;
import com.loja.pedidos.dominio.Produto;
import com.loja.pedidos.dominio.Resumo;
import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;
import com.loja.pedidos.persistencia.AvisoRepositorio;
import com.loja.pedidos.persistencia.ClienteRepositorio;
import com.loja.pedidos.persistencia.ProdutoRepositorio;

@RestController
public class PedidosController {

    public record PagamentoRequest(String idTransacao, Instant pagoEm) {
    }

    public record EnvioRequest(String codigoRastreio) {
    }

    private final PedidoServico servico;
    private final ProdutoRepositorio produtos;
    private final ClienteRepositorio clientes;
    private final AvisoRepositorio avisos;

    public PedidosController(PedidoServico servico, ProdutoRepositorio produtos, ClienteRepositorio clientes,
            AvisoRepositorio avisos) {
        this.servico = servico;
        this.produtos = produtos;
        this.clientes = clientes;
        this.avisos = avisos;
    }

    @PostMapping("/checkout/resumo")
    Resumo resumo(@RequestBody CompraRequest compra) {
        return servico.resumo(compra);
    }

    @PostMapping("/pedidos")
    ResponseEntity<PedidoResposta> registrar(@RequestBody CompraRequest compra) {
        return ResponseEntity.status(201).body(PedidoResposta.de(servico.registrar(compra)));
    }

    @GetMapping("/pedidos/{id}")
    PedidoResposta consultar(@PathVariable String id) {
        return PedidoResposta.de(servico.consultar(id));
    }

    @PostMapping("/pedidos/{id}/pagamento")
    PedidoResposta pagar(@PathVariable String id, @RequestBody(required = false) PagamentoRequest corpo) {
        return PedidoResposta.de(servico.pagar(id,
                corpo == null ? null : corpo.idTransacao(), corpo == null ? null : corpo.pagoEm()));
    }

    @PostMapping("/pedidos/{id}/envio")
    PedidoResposta enviar(@PathVariable String id, @RequestBody(required = false) EnvioRequest corpo) {
        return PedidoResposta.de(servico.enviar(id, corpo == null ? null : corpo.codigoRastreio()));
    }

    @PostMapping("/pedidos/{id}/entrega")
    PedidoResposta entregar(@PathVariable String id) {
        return PedidoResposta.de(servico.entregar(id));
    }

    @PostMapping("/pedidos/{id}/cancelamento")
    PedidoResposta cancelar(@PathVariable String id) {
        return PedidoResposta.de(servico.cancelar(id));
    }

    @GetMapping("/produtos/{sku}")
    Produto produto(@PathVariable String sku) {
        return produtos.buscar(sku).orElseThrow(() -> new ErroNegocio(CodigoErro.PRODUTO_NAO_ENCONTRADO));
    }

    @GetMapping("/clientes/{cpf}")
    Map<String, Object> cliente(@PathVariable String cpf) {
        Cliente c = buscarCliente(cpf);
        return Map.of("cpf", c.cpf(), "saldoCredito", c.saldoCredito());
    }

    @GetMapping("/clientes/{cpf}/avisos")
    List<Aviso> avisos(@PathVariable String cpf) {
        buscarCliente(cpf);
        return avisos.listar(cpf);
    }

    @PostMapping("/avisos/{id}/leitura")
    Aviso marcarLido(@PathVariable String id) {
        return avisos.marcarLido(id).orElseThrow(() -> new ErroNegocio(CodigoErro.AVISO_NAO_ENCONTRADO));
    }

    private Cliente buscarCliente(String cpf) {
        return clientes.buscar(cpf).orElseThrow(() -> new ErroNegocio(CodigoErro.CLIENTE_NAO_ENCONTRADO));
    }
}
