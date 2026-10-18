package br.com.loja.pedidos.shipping;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ShippingConfig {

    @Bean
    ShippingOption economyShipping(TransLogClient client) {
        return new TransLogShipping("ECONOMY", "STD", client);
    }

    @Bean
    ShippingOption expressShipping(TransLogClient client) {
        return new TransLogShipping("EXPRESS", "EXP", client);
    }
}
