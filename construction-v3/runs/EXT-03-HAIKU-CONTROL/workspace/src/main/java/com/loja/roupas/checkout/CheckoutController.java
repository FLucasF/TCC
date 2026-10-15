package com.loja.roupas.checkout;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService = new CheckoutService();

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutService.ValidationException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getCodigoErro()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("ERRO_INTERNO"));
        }
    }
}
