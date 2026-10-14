package br.com.loja.pedidos.pedido;

import br.com.loja.pedidos.checkout.DadosCompra;
import br.com.loja.pedidos.checkout.Resumo;
import br.com.loja.pedidos.comum.ErroNegocio;
import br.com.loja.pedidos.comum.Json;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
    public ResponseEntity<PedidoResposta> registrar(@RequestBody(required = false) Object corpo) {
        Pedido pedido = servico.registrar(DadosCompra.ler(corpo));
        return ResponseEntity.created(URI.create("/pedidos/" + pedido.id())).body(PedidoResposta.de(pedido));
    }

    @GetMapping("/{id}")
    public PedidoResposta consultar(@PathVariable String id) {
        return PedidoResposta.de(servico.consultar(id(id)));
    }

    @PostMapping("/{id}/pagamento")
    public PedidoResposta pagamento(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corpo) {
        Map<String, Object> dados = corpo == null ? Map.of() : corpo;
        return PedidoResposta.de(servico.registrarPagamento(id(id), Json.texto(dados.get("idTransacao")), Json.instante(dados.get("pagoEm"))));
    }

    @PostMapping("/{id}/envio")
    public PedidoResposta envio(@PathVariable String id, @RequestBody(required = false) Map<String, Object> corpo) {
        Map<String, Object> dados = corpo == null ? Map.of() : corpo;
        return PedidoResposta.de(servico.enviar(id(id), Json.texto(dados.get("codigoRastreio"))));
    }

    @PostMapping("/{id}/entrega")
    public PedidoResposta entrega(@PathVariable String id) {
        return PedidoResposta.de(servico.entregar(id(id)));
    }

    @PostMapping("/{id}/cancelamento")
    public PedidoResposta cancelamento(@PathVariable String id) {
        return PedidoResposta.de(servico.cancelar(id(id)));
    }

    private static long id(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ErroNegocio.naoEncontrado("PEDIDO_NAO_ENCONTRADO");
        }
    }

    public record PedidoResposta(long id, String situacao, String criadoEm, Resumo resumo, List<Item> itens,
                                 String codigoRastreio) {

        static PedidoResposta de(Pedido pedido) {
            return new PedidoResposta(
                    pedido.id(),
                    pedido.situacao().name(),
                    DateTimeFormatter.ISO_INSTANT.format(pedido.criadoEm()),
                    pedido.resumo(),
                    pedido.itens().stream().map(item -> new Item(item.sku(), item.quantidade())).toList(),
                    pedido.codigoRastreio());
        }

        public record Item(String sku, int quantidade) {
        }
    }
}
