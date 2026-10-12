package com.loja.checkout.controller;

import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.service.CheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {
  private CheckoutService checkoutService;

  public CheckoutController(CheckoutService checkoutService) {
    this.checkoutService = checkoutService;
  }

  @PostMapping("/checkout/resumo")
  public Object resumo(@RequestBody CheckoutRequest request) {
    return checkoutService.processar(request);
  }
}
