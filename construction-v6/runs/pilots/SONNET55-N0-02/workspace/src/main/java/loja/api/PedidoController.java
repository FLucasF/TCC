package loja.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import loja.dominio.Resumo;
import loja.pedido.PedidoService;

@RestController
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    record Pagamento(String idTransacao, String pagoEm) {
    }

    record Envio(String codigoRastreio) {
    }

    @PostMapping("/checkout/resumo")
    public Resumo resumo(@RequestBody(required = false) PedidoRequest req) {
        return service.resumo(req);
    }

    @PostMapping("/pedidos")
    public ResponseEntity<PedidoDto> registrar(@RequestBody(required = false) PedidoRequest req) {
        return ResponseEntity.status(201).body(PedidoDto.de(service.registrar(req)));
    }

    @GetMapping("/pedidos/{id}")
    public PedidoDto consultar(@PathVariable String id) {
        return PedidoDto.de(service.consultar(id));
    }

    @PostMapping("/pedidos/{id}/pagamento")
    public PedidoDto pagar(@PathVariable String id, @RequestBody(required = false) Pagamento body) {
        if (body == null) {
            body = new Pagamento(null, null);
        }
        return PedidoDto.de(service.pagar(id, body.idTransacao(), body.pagoEm()));
    }

    @PostMapping("/pedidos/{id}/envio")
    public PedidoDto enviar(@PathVariable String id, @RequestBody(required = false) Envio body) {
        return PedidoDto.de(service.enviar(id, body == null ? null : body.codigoRastreio()));
    }

    @PostMapping("/pedidos/{id}/entrega")
    public PedidoDto entregar(@PathVariable String id) {
        return PedidoDto.de(service.entregar(id));
    }

    @PostMapping("/pedidos/{id}/cancelamento")
    public PedidoDto cancelar(@PathVariable String id) {
        return PedidoDto.de(service.cancelar(id));
    }
}
