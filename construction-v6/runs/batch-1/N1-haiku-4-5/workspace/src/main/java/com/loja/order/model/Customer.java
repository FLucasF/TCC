package com.loja.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customers")
public class Customer {
  @Id
  @Column(name = "cpf", length = 11)
  private String cpf;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "credit_balance", nullable = false, precision = 10, scale = 2)
  private BigDecimal creditBalance;

  protected Customer() {}

  public Customer(String cpf, String email) {
    this.cpf = cpf;
    this.email = email;
    this.creditBalance = BigDecimal.ZERO;
  }

  public String getCpf() {
    return cpf;
  }

  public String getEmail() {
    return email;
  }

  public BigDecimal getCreditBalance() {
    return creditBalance;
  }

  public void addCredit(BigDecimal amount) {
    this.creditBalance = creditBalance.add(amount);
  }

  public void removeCredit(BigDecimal amount) {
    this.creditBalance = creditBalance.subtract(amount);
  }
}
