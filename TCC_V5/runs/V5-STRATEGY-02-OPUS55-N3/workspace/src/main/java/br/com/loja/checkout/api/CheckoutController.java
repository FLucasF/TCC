package br.com.loja.checkout.api;

import br.com.loja.checkout.CheckoutService;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.Resumo;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public Resumo resumo(@RequestBody ResumoRequest pedido) {
        return service.calcular(pedido);
    }

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErroResponse recusado(CheckoutException e) {
        return new ErroResponse(e.codigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErroResponse ilegivel() {
        return new ErroResponse("PEDIDO_INVALIDO");
    }
}
