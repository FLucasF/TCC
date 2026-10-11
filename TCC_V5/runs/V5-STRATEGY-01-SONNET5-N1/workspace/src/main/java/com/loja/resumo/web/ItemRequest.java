package com.loja.resumo.web;

public record ItemRequest(String nome, Double precoUnitario, Integer quantidade, Double pesoKg) {
}
