package br.com.loja.checkout;

import br.com.loja.checkout.pedido.PedidoRequest;
import br.com.loja.checkout.pedido.ResumoCompra;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckoutController {

    private final ResumoCompraService service;

    public CheckoutController(ResumoCompraService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompra resumo(@RequestBody PedidoRequest pedido) {
        return service.calcular(pedido);
    }

    @ExceptionHandler(PedidoRecusadoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public Map<String, String> recusado(PedidoRecusadoException e) {
        return Map.of("erro", e.codigo());
    }
}
