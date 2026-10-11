package br.com.loja.checkout.api;

import br.com.loja.checkout.dominio.ErroNegocio;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoService service;

    public CheckoutController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return service.calcular(request);
    }

    @ExceptionHandler(ErroNegocio.class)
    public ResponseEntity<Map<String, String>> erro(ErroNegocio e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Map.of("erro", e.codigo()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoIlegivel() {
        return ResponseEntity.badRequest().body(Map.of("erro", "PEDIDO_INVALIDO"));
    }
}
