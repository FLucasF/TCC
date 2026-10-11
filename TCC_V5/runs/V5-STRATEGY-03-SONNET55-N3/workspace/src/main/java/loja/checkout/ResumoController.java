package loja.checkout;

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
    private final ResumoService service;

    public ResumoController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody PedidoRequest pedido) {
        return service.calcular(pedido);
    }

    @ExceptionHandler(ErroNegocio.class)
    public ResponseEntity<Map<String, String>> erro(ErroNegocio e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.codigo()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoIlegivel() {
        return erro(new ErroNegocio("PEDIDO_INVALIDO"));
    }
}
