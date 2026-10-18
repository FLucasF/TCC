package com.loja.domain.entities;

public class Product {
    private final String sku;
    private final String name;
    private final Money price;
    private final double weightKg;
    private int stock;
    private final boolean discontinued;

    public Product(String sku, String name, Money price, double weightKg, int stock, boolean discontinued) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.weightKg = weightKg;
        this.stock = stock;
        this.discontinued = discontinued;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public int getStock() {
        return stock;
    }

    public boolean isDiscontinued() {
        return discontinued;
    }

    public boolean hasStock(int quantity) {
        return stock >= quantity;
    }

    public void decreaseStock(int quantity) {
        if (quantity > stock) {
            throw new IllegalStateException("Insufficient stock for product " + sku);
        }
        stock -= quantity;
    }

    public void increaseStock(int quantity) {
        stock += quantity;
    }
}
