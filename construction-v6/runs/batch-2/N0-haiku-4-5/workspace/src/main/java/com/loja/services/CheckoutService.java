package com.loja.services;

import com.loja.api.CheckoutRequest;
import com.loja.database.Database;
import com.loja.models.*;
import com.loja.utils.MoneyUtils;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class CheckoutService {
    private static final Pattern ZIP_CODE_PATTERN = Pattern.compile("^\\d{8}$");
    private static final double MAX_COURIER_WEIGHT = 5.0;
    private static final double MAX_DISCOUNT_PERCENTAGE = 0.30;
    private static final long PAYMENT_TIMEOUT_MS = 48 * 60 * 60 * 1000;

    public static ValidationError validateCheckoutRequest(CheckoutRequest req) throws SQLException {
        if (req.items == null || req.items.isEmpty()) {
            return new ValidationError("INVALID_ORDER", 422);
        }

        if (req.customer == null || req.customer.cpf == null || req.customer.cpf.isEmpty() ||
            req.customer.email == null || req.customer.email.isEmpty()) {
            return new ValidationError("INVALID_ORDER", 422);
        }

        Set<String> seenSkus = new HashSet<>();
        for (CheckoutRequest.OrderItemData item : req.items) {
            if (item.sku == null || item.sku.isEmpty() || item.quantity <= 0) {
                return new ValidationError("INVALID_ORDER", 422);
            }
            if (seenSkus.contains(item.sku)) {
                return new ValidationError("INVALID_ORDER", 422);
            }
            seenSkus.add(item.sku);
        }

        for (CheckoutRequest.OrderItemData item : req.items) {
            Product product = ProductService.getProduct(item.sku);
            if (product == null) {
                return new ValidationError("UNKNOWN_PRODUCT", 422);
            }
            if (product.discontinued) {
                return new ValidationError("PRODUCT_DISCONTINUED", 422);
            }
            if (product.stock < item.quantity) {
                return new ValidationError("INSUFFICIENT_STOCK", 422);
            }
        }

        if (req.clubTier == null || req.clubTier.isEmpty()) {
            return new ValidationError("INVALID_CLUB_TIER", 422);
        }
        try {
            Enums.ClubTier.valueOf(req.clubTier);
        } catch (IllegalArgumentException e) {
            return new ValidationError("INVALID_CLUB_TIER", 422);
        }

        if (req.region == null || req.region.isEmpty()) {
            return new ValidationError("INVALID_REGION", 422);
        }
        try {
            Enums.Region.valueOf(req.region);
        } catch (IllegalArgumentException e) {
            return new ValidationError("INVALID_REGION", 422);
        }

        if (req.zipCode == null || !ZIP_CODE_PATTERN.matcher(req.zipCode).matches()) {
            return new ValidationError("INVALID_ZIP_CODE", 422);
        }

        if (req.shippingMethod == null || req.shippingMethod.isEmpty()) {
            return new ValidationError("INVALID_SHIPPING_METHOD", 422);
        }
        try {
            Enums.ShippingMethod.valueOf(req.shippingMethod);
        } catch (IllegalArgumentException e) {
            return new ValidationError("INVALID_SHIPPING_METHOD", 422);
        }

        ValidationError shippingError = validateShippingMethod(req);
        if (shippingError != null) {
            return shippingError;
        }

        if (req.paymentMethod == null || req.paymentMethod.isEmpty()) {
            return new ValidationError("INVALID_PAYMENT_METHOD", 422);
        }
        try {
            Enums.PaymentMethod.valueOf(req.paymentMethod);
        } catch (IllegalArgumentException e) {
            return new ValidationError("INVALID_PAYMENT_METHOD", 422);
        }

        int installments = req.installments != null ? req.installments : 1;
        if (!isInstallmentsValid(req.paymentMethod, installments)) {
            return new ValidationError("INVALID_INSTALLMENTS", 422);
        }

        if (req.coupon != null && !req.coupon.isEmpty() && !isCouponValid(req.coupon)) {
            return new ValidationError("INVALID_COUPON", 422);
        }

        ValidationError couponError = validateCouponApplicability(req);
        if (couponError != null) {
            return couponError;
        }

        ValidationError paymentError = validatePaymentMethod(req);
        if (paymentError != null) {
            return paymentError;
        }

        return null;
    }

    private static ValidationError validateShippingMethod(CheckoutRequest req) throws SQLException {
        Enums.ShippingMethod method = Enums.ShippingMethod.valueOf(req.shippingMethod);

        if (method == Enums.ShippingMethod.COURIER) {
            double totalWeight = 0;
            for (CheckoutRequest.OrderItemData item : req.items) {
                Product product = ProductService.getProduct(item.sku);
                totalWeight += product.weightKg * item.quantity;
            }
            if (totalWeight > MAX_COURIER_WEIGHT) {
                return new ValidationError("SHIPPING_METHOD_UNAVAILABLE", 422);
            }
        }

        if (method == Enums.ShippingMethod.ECONOMY || method == Enums.ShippingMethod.EXPRESS) {
            String translogUrl = System.getenv("TRANSLOG_URL");
            if (translogUrl == null || translogUrl.isEmpty()) {
                return null;
            }

            double totalWeight = 0;
            for (CheckoutRequest.OrderItemData item : req.items) {
                Product product = ProductService.getProduct(item.sku);
                totalWeight += product.weightKg * item.quantity;
            }

            String service = method == Enums.ShippingMethod.ECONOMY ? "STD" : "EXP";
            int weightGrams = (int) (totalWeight * 1000);

            TranslogResponse response = queryTranslog(translogUrl, req.zipCode, weightGrams, service);
            if (response == null) {
                return new ValidationError("CARRIER_UNAVAILABLE", 503);
            }
            if (response.error != null && response.error.equals("ZIP_NOT_SERVED")) {
                return new ValidationError("SHIPPING_METHOD_UNAVAILABLE", 422);
            }
        }

        return null;
    }

    private static ValidationError validateCouponApplicability(CheckoutRequest req) throws SQLException {
        if (req.coupon == null || req.coupon.isEmpty()) {
            return null;
        }

        double subtotal = 0;
        for (CheckoutRequest.OrderItemData item : req.items) {
            Product product = ProductService.getProduct(item.sku);
            subtotal += product.price * item.quantity;
        }
        subtotal = MoneyUtils.round(subtotal);

        if (req.coupon.equals("WELCOME10")) {
            boolean isFirstPurchase = !hasCompletedOrder(req.customer.cpf);
            if (!isFirstPurchase) {
                return new ValidationError("COUPON_NOT_APPLICABLE", 422);
            }
        } else if (req.coupon.equals("MINUS50")) {
            if (subtotal < 300) {
                return new ValidationError("COUPON_NOT_APPLICABLE", 422);
            }
        } else if (req.coupon.equals("FREESHIPPING")) {
            if (req.shippingMethod.equals("STORE_PICKUP")) {
                return new ValidationError("COUPON_NOT_APPLICABLE", 422);
            }
        }

        return null;
    }

    private static ValidationError validatePaymentMethod(CheckoutRequest req) throws SQLException {
        if (req.paymentMethod.equals("BANK_SLIP")) {
            OrderSummary summary = calculateSummary(req);
            if (summary.finalTotal > 1000.0) {
                return new ValidationError("PAYMENT_METHOD_UNAVAILABLE", 422);
            }
        }
        return null;
    }

    public static OrderSummary calculateSummary(CheckoutRequest req) throws SQLException {
        double subtotal = 0;
        for (CheckoutRequest.OrderItemData item : req.items) {
            Product product = ProductService.getProduct(item.sku);
            subtotal += product.price * item.quantity;
        }
        subtotal = MoneyUtils.round(subtotal);

        double couponDiscount = 0;
        if (req.coupon != null && !req.coupon.isEmpty()) {
            couponDiscount = calculateCouponDiscount(req, subtotal);
            double maxDiscount = MoneyUtils.round(subtotal * MAX_DISCOUNT_PERCENTAGE);
            if (couponDiscount > maxDiscount) {
                couponDiscount = maxDiscount;
            }
        }

        double shippingCost = 0;
        int deliveryDays = 0;
        ShippingInfo shippingInfo = calculateShipping(req);
        if (shippingInfo != null) {
            shippingCost = shippingInfo.cost;
            deliveryDays = shippingInfo.days;
        }

        if (Enums.ClubTier.valueOf(req.clubTier) == Enums.ClubTier.GOLD &&
            !req.shippingMethod.equals("COURIER") &&
            !req.shippingMethod.equals("STORE_PICKUP")) {
            shippingCost = 0;
        }

        double insurance = calculateInsurance(req, subtotal);

        double totalBeforePayment = subtotal - couponDiscount + shippingCost + insurance;
        totalBeforePayment = MoneyUtils.round(totalBeforePayment);

        double paymentAdjustment = 0;
        int installments = req.installments != null ? req.installments : 1;
        double finalTotal = totalBeforePayment;

        if (req.coupon == null || req.coupon.isEmpty()) {
            if (req.paymentMethod.equals("PIX")) {
                double pixDiscount = MoneyUtils.round(totalBeforePayment * 0.05);
                paymentAdjustment = -pixDiscount;
                finalTotal = MoneyUtils.round(totalBeforePayment - pixDiscount);
            } else if (req.paymentMethod.equals("BANK_SLIP")) {
                paymentAdjustment = 3.49;
                finalTotal = MoneyUtils.round(totalBeforePayment + 3.49);
            }
        } else if (req.paymentMethod.equals("BANK_SLIP")) {
            paymentAdjustment = 3.49;
            finalTotal = MoneyUtils.round(totalBeforePayment + 3.49);
        }

        double installmentAmount = finalTotal;
        if (req.paymentMethod.equals("CREDIT_CARD") && installments > 3) {
            double monthlyRate = 0.0199;
            double factor = Math.pow(1 + monthlyRate, -installments);
            installmentAmount = MoneyUtils.round(finalTotal * monthlyRate / (1 - factor));
            finalTotal = MoneyUtils.round(installmentAmount * installments);
        } else if (installments > 1) {
            installmentAmount = MoneyUtils.round(finalTotal / installments);
            finalTotal = MoneyUtils.round(installmentAmount * installments);
        }

        double credit = 0;
        Enums.ClubTier tier = Enums.ClubTier.valueOf(req.clubTier);
        if (tier == Enums.ClubTier.SILVER) {
            credit = MoneyUtils.round(subtotal * 0.02);
        } else if (tier == Enums.ClubTier.GOLD) {
            credit = MoneyUtils.round(subtotal * 0.05);
        }

        boolean gift = tier == Enums.ClubTier.GOLD && subtotal > 500;

        return new OrderSummary(
            subtotal, couponDiscount, shippingCost, deliveryDays, insurance,
            paymentAdjustment, finalTotal, installments, installmentAmount,
            credit, gift
        );
    }

    private static double calculateCouponDiscount(CheckoutRequest req, double subtotal) throws SQLException {
        String coupon = req.coupon;

        if (coupon.equals("WELCOME10")) {
            boolean isFirstPurchase = !hasCompletedOrder(req.customer.cpf);
            if (!isFirstPurchase) {
                return 0;
            }
            return MoneyUtils.round(subtotal * 0.10);
        }

        if (coupon.equals("MINUS50")) {
            if (subtotal < 300) {
                return 0;
            }
            return 50.0;
        }

        if (coupon.equals("FREESHIPPING")) {
            if (req.shippingMethod.equals("STORE_PICKUP")) {
                return 0;
            }
            ShippingInfo shipping = calculateShipping(req);
            if (shipping == null) {
                return 0;
            }
            return shipping.cost;
        }

        if (coupon.equals("BUY3PAY2")) {
            double discount = 0;
            for (CheckoutRequest.OrderItemData item : req.items) {
                Product product = ProductService.getProduct(item.sku);
                int freeUnits = item.quantity / 3;
                discount += product.price * freeUnits;
            }
            return MoneyUtils.round(discount);
        }

        return 0;
    }

    private static boolean hasCompletedOrder(String cpf) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM orders WHERE cpf = ? AND (status = 'PAID' OR status = 'SHIPPED' OR status = 'DELIVERED')";
        java.sql.Connection conn = Database.getDataSource().getConnection();
        java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, cpf);
        java.sql.ResultSet rs = pstmt.executeQuery();
        rs.next();
        int count = rs.getInt("count");
        rs.close();
        pstmt.close();
        conn.close();
        return count > 0;
    }

    private static ShippingInfo calculateShipping(CheckoutRequest req) throws SQLException {
        Enums.ShippingMethod method = Enums.ShippingMethod.valueOf(req.shippingMethod);

        double totalWeight = 0;
        for (CheckoutRequest.OrderItemData item : req.items) {
            Product product = ProductService.getProduct(item.sku);
            totalWeight += product.weightKg * item.quantity;
        }

        if (method == Enums.ShippingMethod.STORE_PICKUP) {
            return new ShippingInfo(0, 1);
        }

        if (method == Enums.ShippingMethod.COURIER) {
            if (totalWeight > MAX_COURIER_WEIGHT) {
                return null;
            }
            return new ShippingInfo(18.0, 0);
        }

        String translogUrl = System.getenv("TRANSLOG_URL");
        if (translogUrl == null || translogUrl.isEmpty()) {
            return null;
        }

        String service = method == Enums.ShippingMethod.ECONOMY ? "STD" : "EXP";
        int weightGrams = (int) (totalWeight * 1000);

        TranslogResponse response = queryTranslog(translogUrl, req.zipCode, weightGrams, service);
        if (response == null) {
            return null;
        }

        if (response.error != null) {
            return null;
        }

        double costReais = response.priceCents / 100.0;
        int daysBase = (int) Math.ceil(response.etaHours / 24.0);

        Enums.Region region = Enums.Region.valueOf(req.region);
        int daysExtra = 0;
        if (region == Enums.Region.NORTH) {
            daysExtra = 3;
        } else if (region == Enums.Region.NORTHEAST) {
            daysExtra = 2;
        }

        return new ShippingInfo(costReais, daysBase + daysExtra);
    }

    private static TranslogResponse queryTranslog(String baseUrl, String zipCode, int weightGrams, String service) {
        try {
            String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s", baseUrl, zipCode, weightGrams, service);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            int status = conn.getResponseCode();
            if (status != 200) {
                return null;
            }

            String response = new String(conn.getInputStream().readAllBytes());
            com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(response);
            com.google.gson.JsonObject obj = element.getAsJsonObject();

            if (obj.has("error")) {
                TranslogResponse resp = new TranslogResponse();
                resp.error = obj.get("error").getAsString();
                return resp;
            }

            TranslogResponse resp = new TranslogResponse();
            resp.priceCents = obj.get("price_cents").getAsInt();
            resp.etaHours = obj.get("eta_hours").getAsInt();
            return resp;
        } catch (Exception e) {
            return null;
        }
    }

    private static double calculateInsurance(CheckoutRequest req, double subtotal) {
        Enums.Region region = Enums.Region.valueOf(req.region);
        double percentage = 0;
        double limit = Double.MAX_VALUE;

        switch (region) {
            case SOUTHEAST, SOUTH -> percentage = 0.01;
            case MIDWEST -> percentage = 0.015;
            case NORTH -> {
                percentage = 0.025;
                limit = 30.0;
            }
            case NORTHEAST -> {
                percentage = 0.02;
                limit = 30.0;
            }
        }

        double insurance = MoneyUtils.round(subtotal * percentage);
        if (insurance > limit) {
            insurance = limit;
        }
        return insurance;
    }

    private static boolean isInstallmentsValid(String paymentMethod, int installments) {
        if (installments < 1 || installments > 12) {
            return false;
        }
        if (paymentMethod.equals("PIX") || paymentMethod.equals("BANK_SLIP")) {
            return installments == 1;
        }
        return true;
    }

    private static boolean isCouponValid(String coupon) {
        return coupon.equals("WELCOME10") || coupon.equals("MINUS50") ||
               coupon.equals("FREESHIPPING") || coupon.equals("BUY3PAY2");
    }

    public static class ValidationError {
        public String code;
        public int httpStatus;

        public ValidationError(String code, int httpStatus) {
            this.code = code;
            this.httpStatus = httpStatus;
        }
    }

    private static class ShippingInfo {
        double cost;
        int days;

        ShippingInfo(double cost, int days) {
            this.cost = cost;
            this.days = days;
        }
    }

    private static class TranslogResponse {
        int priceCents;
        int etaHours;
        String error;
    }
}
