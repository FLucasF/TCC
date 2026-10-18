package com.loja.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Product {
    public String sku;
    public String name;
    public Double price;
    public Double weightKg;
    public Integer stock;
    public Boolean discontinued;

    public Product() {}

    public Product(String sku, String name, Double price, Double weightKg, Integer stock, Boolean discontinued) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.weightKg = weightKg;
        this.stock = stock;
        this.discontinued = discontinued;
    }
}
