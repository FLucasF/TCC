package com.shop.dto;

import com.shop.model.OrderItem;
import com.shop.model.enums.ClubTier;
import com.shop.model.enums.CouponCode;
import com.shop.model.enums.PaymentMethod;
import com.shop.model.enums.Region;
import com.shop.model.enums.ShippingMethod;
import java.util.List;

public class CheckoutRequest {
    private CustomerInfo customer;
    private List<OrderItem> items;
    private String zipCode;
    private Region region;
    private ShippingMethod shippingMethod;
    private CouponCode coupon;
    private PaymentMethod paymentMethod;
    private Integer installments;
    private ClubTier clubTier;

    public CheckoutRequest() {}

    public CustomerInfo getCustomer() { return customer; }
    public void setCustomer(CustomerInfo customer) { this.customer = customer; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public ShippingMethod getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(ShippingMethod shippingMethod) { this.shippingMethod = shippingMethod; }

    public CouponCode getCoupon() { return coupon; }
    public void setCoupon(CouponCode coupon) { this.coupon = coupon; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }

    public ClubTier getClubTier() { return clubTier; }
    public void setClubTier(ClubTier clubTier) { this.clubTier = clubTier; }
}
