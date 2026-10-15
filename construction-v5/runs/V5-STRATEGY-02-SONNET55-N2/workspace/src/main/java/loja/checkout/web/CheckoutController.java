package loja.checkout.web;

import java.util.Map;
import loja.checkout.ErroPedido;
import loja.checkout.ResumoService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> resumo(@RequestBody PedidoRequest pedido) {
        try {
            return ResponseEntity.ok(service.calcular(pedido));
        } catch (ErroPedido e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.codigo()));
        }
    }
}
