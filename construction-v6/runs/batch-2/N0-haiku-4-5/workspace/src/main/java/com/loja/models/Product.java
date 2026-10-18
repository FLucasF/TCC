package com.loja.models;

public class Product {
    public String sku;
    public String name;
    public double price;
    public double weightKg;
    public int stock;
    public boolean discontinued;

    public Product(String sku, String name, double price, double weightKg, int stock, boolean discontinued) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.weightKg = weightKg;
        this.stock = stock;
        this.discontinued = discontinued;
    }
}
