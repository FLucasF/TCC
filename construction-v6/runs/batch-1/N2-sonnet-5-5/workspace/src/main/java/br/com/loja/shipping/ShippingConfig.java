package br.com.loja.shipping;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ShippingConfig {

    @Bean
    ShippingOption economyShipping(CarrierGateway gateway) {
        return new CarrierShipping("ECONOMY", "STD", gateway);
    }

    @Bean
    ShippingOption expressShipping(CarrierGateway gateway) {
        return new CarrierShipping("EXPRESS", "EXP", gateway);
    }
}
