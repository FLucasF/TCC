package com.loja.checkout.web;

import com.loja.checkout.domain.CheckoutService;
import com.loja.checkout.domain.CodigoErro;
import com.loja.checkout.domain.PedidoInvalidoException;
import com.loja.checkout.domain.Resumo;
import com.loja.checkout.web.dto.CheckoutRequest;
import com.loja.checkout.web.dto.CheckoutResponse;
import com.loja.checkout.web.dto.ErroResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest req) {
        Resumo resumo = service.calcular(req);
        return ResponseEntity.ok(CheckoutResponse.de(resumo));
    }

    @ExceptionHandler(PedidoInvalidoException.class)
    public ResponseEntity<ErroResponse> onInvalido(PedidoInvalidoException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(e.codigo().name()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> onJsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
