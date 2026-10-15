package br.com.loja.checkout;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final ServicoResumo servico;

    public ResumoController(ServicoResumo servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest pedido) {
        return servico.calcular(pedido);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoIlegivel() {
        return ResponseEntity.badRequest().body(Map.of("erro", CodigoErro.PEDIDO_INVALIDO.name()));
    }

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<Map<String, String>> recusado(PedidoRecusadoException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("erro", e.codigo().name()));
    }
}
