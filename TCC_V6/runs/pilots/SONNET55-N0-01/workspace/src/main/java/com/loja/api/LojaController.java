package com.loja.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.loja.api.Visoes.AvisoVisao;
import com.loja.api.Visoes.ClienteVisao;
import com.loja.api.Visoes.PedidoVisao;
import com.loja.api.Visoes.ProdutoVisao;
import com.loja.dominio.ConsultaService;
import com.loja.dominio.DadosCompra;
import com.loja.dominio.PedidoService;
import com.loja.dominio.Resumo;

@RestController
public class LojaController {

    record PagamentoDados(String idTransacao, String pagoEm) {
    }

    record EnvioDados(String codigoRastreio) {
    }

    private final PedidoService pedidos;
    private final ConsultaService consultas;

    public LojaController(PedidoService pedidos, ConsultaService consultas) {
        this.pedidos = pedidos;
        this.consultas = consultas;
    }

    @PostMapping("/checkout/resumo")
    public Resumo resumo(@RequestBody(required = false) DadosCompra dados) {
        return pedidos.resumir(dados);
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoVisao registrar(@RequestBody(required = false) DadosCompra dados) {
        return PedidoVisao.de(pedidos.registrar(dados));
    }

    @GetMapping("/pedidos/{id}")
    public PedidoVisao consultar(@PathVariable String id) {
        return PedidoVisao.de(pedidos.consultar(id));
    }

    @PostMapping("/pedidos/{id}/pagamento")
    public PedidoVisao pagar(@PathVariable String id, @RequestBody(required = false) PagamentoDados dados) {
        return PedidoVisao.de(pedidos.pagar(id, dados == null ? null : dados.idTransacao(),
                dados == null ? null : dados.pagoEm()));
    }

    @PostMapping("/pedidos/{id}/envio")
    public PedidoVisao enviar(@PathVariable String id, @RequestBody(required = false) EnvioDados dados) {
        return PedidoVisao.de(pedidos.enviar(id, dados == null ? null : dados.codigoRastreio()));
    }

    @PostMapping("/pedidos/{id}/entrega")
    public PedidoVisao entregar(@PathVariable String id) {
        return PedidoVisao.de(pedidos.entregar(id));
    }

    @PostMapping("/pedidos/{id}/cancelamento")
    public PedidoVisao cancelar(@PathVariable String id) {
        return PedidoVisao.de(pedidos.cancelar(id));
    }

    @GetMapping("/produtos/{sku}")
    public ProdutoVisao produto(@PathVariable String sku) {
        return ProdutoVisao.de(consultas.produto(sku));
    }

    @GetMapping("/clientes/{cpf}")
    public ClienteVisao cliente(@PathVariable String cpf) {
        return ClienteVisao.de(consultas.cliente(cpf));
    }

    @GetMapping("/clientes/{cpf}/avisos")
    public List<AvisoVisao> avisos(@PathVariable String cpf) {
        return consultas.avisos(cpf).stream().map(AvisoVisao::de).toList();
    }

    @PostMapping("/avisos/{id}/leitura")
    public AvisoVisao marcarLido(@PathVariable String id) {
        return AvisoVisao.de(consultas.marcarLido(id));
    }
}
