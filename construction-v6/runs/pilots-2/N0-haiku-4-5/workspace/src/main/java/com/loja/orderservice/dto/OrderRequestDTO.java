package com.loja.orderservice.dto;

import com.loja.orderservice.entity.*;
import java.util.List;

public class OrderRequestDTO {
    private CustomerDTO customer;
    private List<OrderItemDTO> items;
    private String zipCode;
    private Region region;
    private ShippingMethod shippingMethod;
    private String coupon;
    private PaymentMethod paymentMethod;
    private Integer installments;
    private ClubTier clubTier;

    public OrderRequestDTO() {}

    public CustomerDTO getCustomer() { return customer; }
    public void setCustomer(CustomerDTO customer) { this.customer = customer; }

    public List<OrderItemDTO> getItems() { return items; }
    public void setItems(List<OrderItemDTO> items) { this.items = items; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public ShippingMethod getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(ShippingMethod shippingMethod) { this.shippingMethod = shippingMethod; }

    public String getCoupon() { return coupon; }
    public void setCoupon(String coupon) { this.coupon = coupon; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }

    public ClubTier getClubTier() { return clubTier; }
    public void setClubTier(ClubTier clubTier) { this.clubTier = clubTier; }
}
