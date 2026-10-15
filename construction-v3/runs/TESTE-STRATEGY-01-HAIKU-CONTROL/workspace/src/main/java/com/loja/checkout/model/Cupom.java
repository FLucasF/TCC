package com.loja.checkout.model;

import java.util.HashMap;
import java.util.Map;

public enum Cupom {
  BEMVINDO10("10% de desconto no valor dos produtos"),
  MENOS50("R$ 50,00 de desconto nos produtos, mínimo R$ 300"),
  FRETEGRATIS("Frete grátis"),
  LEVE3PAGUE2("A cada 3 unidades de um item, uma é grátis");

  private final String descricao;
  private static final Map<String, Cupom> lookup = new HashMap<>();

  static {
    for (Cupom c : Cupom.values()) {
      lookup.put(c.name(), c);
    }
  }

  Cupom(String descricao) {
    this.descricao = descricao;
  }

  public String getDescricao() {
    return descricao;
  }

  public static Cupom get(String code) {
    return lookup.get(code);
  }

  public static boolean isValid(String code) {
    return lookup.containsKey(code);
  }
}
