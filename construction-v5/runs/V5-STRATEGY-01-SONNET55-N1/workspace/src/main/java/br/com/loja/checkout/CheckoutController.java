package br.com.loja.checkout;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    public record ErroResponse(String erro) {
    }

    private final ResumoService service;

    public CheckoutController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return service.calcular(request);
    }

    @ExceptionHandler(ErroNegocio.class)
    ResponseEntity<ErroResponse> erroNegocio(ErroNegocio e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErroResponse(e.codigo().name()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoIlegivel() {
        return ResponseEntity.badRequest().body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
