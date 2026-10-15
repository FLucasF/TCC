package br.tcc.checkout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.tcc.checkout.dto.CheckoutError;
import br.tcc.checkout.dto.CheckoutRequest;
import br.tcc.checkout.dto.CheckoutResponse;
import br.tcc.checkout.service.CheckoutException;
import br.tcc.checkout.service.CheckoutService;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutException e) {
            CheckoutError error = new CheckoutError(e.getCodigo());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
