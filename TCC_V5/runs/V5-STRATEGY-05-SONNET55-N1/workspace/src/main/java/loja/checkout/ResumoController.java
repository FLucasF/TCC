package loja.checkout;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ResumoController {

    private final ResumoService service;

    public ResumoController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return service.calcular(request);
    }

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<Map<String, String>> recusado(PedidoRecusadoException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("erro", e.erro().name()));
    }
}
