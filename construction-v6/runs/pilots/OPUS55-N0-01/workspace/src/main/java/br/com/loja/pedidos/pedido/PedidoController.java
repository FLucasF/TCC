package br.com.loja.pedidos.pedido;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;

@RestController
public class PedidoController {

    private final PedidoService servico;

    public PedidoController(PedidoService servico) {
        this.servico = servico;
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResposta registrar(@RequestBody(required = false) JsonNode dados) {
        return PedidoResposta.de(servico.registrar(dados));
    }

    @GetMapping("/pedidos/{id}")
    public PedidoResposta consultar(@PathVariable String id) {
        return PedidoResposta.de(servico.consultar(id));
    }

    @PostMapping("/pedidos/{id}/pagamento")
    public PedidoResposta pagar(@PathVariable String id, @RequestBody(required = false) JsonNode corpo) {
        return PedidoResposta.de(servico.pagar(id, texto(corpo, "idTransacao"), instante(texto(corpo, "pagoEm"))));
    }

    @PostMapping("/pedidos/{id}/envio")
    public PedidoResposta enviar(@PathVariable String id, @RequestBody(required = false) JsonNode corpo) {
        return PedidoResposta.de(servico.enviar(id, texto(corpo, "codigoRastreio")));
    }

    @PostMapping("/pedidos/{id}/entrega")
    public PedidoResposta entregar(@PathVariable String id) {
        return PedidoResposta.de(servico.entregar(id));
    }

    @PostMapping("/pedidos/{id}/cancelamento")
    public PedidoResposta cancelar(@PathVariable String id) {
        return PedidoResposta.de(servico.cancelar(id));
    }

    private static String texto(JsonNode corpo, String campo) {
        JsonNode valor = corpo == null ? null : corpo.get(campo);
        return valor != null && valor.isString() ? valor.stringValue() : null;
    }

    private static Instant instante(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
