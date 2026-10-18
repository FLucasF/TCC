package br.com.loja.pedidos.shipping;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ShippingConfig {

    @Bean
    ShippingOption economyShipping(CarrierGateway carrier) {
        return new CarrierShippingOption("ECONOMY", ServiceLevel.STANDARD, carrier);
    }

    @Bean
    ShippingOption expressShipping(CarrierGateway carrier) {
        return new CarrierShippingOption("EXPRESS", ServiceLevel.EXPRESS, carrier);
    }
}
