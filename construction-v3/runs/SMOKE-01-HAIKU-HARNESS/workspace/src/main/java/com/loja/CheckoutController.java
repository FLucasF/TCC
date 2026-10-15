package com.loja;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loja.model.CheckoutRequest;
import com.loja.model.CheckoutResponse;
import com.loja.model.ErrorResponse;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = checkoutService.calculate(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getErrorCode()));
        }
    }
}
