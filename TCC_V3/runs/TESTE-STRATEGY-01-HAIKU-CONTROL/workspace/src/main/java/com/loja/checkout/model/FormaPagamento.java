package com.loja.checkout.model;

public enum FormaPagamento {
  PIX,
  CARTAO,
  BOLETO;

  public static boolean isValid(String code) {
    try {
      FormaPagamento.valueOf(code);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
