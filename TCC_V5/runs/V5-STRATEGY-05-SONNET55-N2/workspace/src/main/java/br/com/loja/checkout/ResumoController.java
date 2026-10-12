package br.com.loja.checkout;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final ResumoCompraService service;

    public ResumoController(ResumoCompraService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompra resumo(@RequestBody RequisicaoResumo requisicao) {
        return service.calcular(requisicao);
    }

    @ExceptionHandler(PedidoRecusadoException.class)
    ResponseEntity<Map<String, String>> recusado(PedidoRecusadoException e) {
        return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.codigo().name()));
    }
}
