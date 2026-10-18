package br.com.loja.pedidos.club;

import org.springframework.stereotype.Component;

@Component
public class BronzeTier implements ClubTier {

    @Override
    public String code() {
        return "BRONZE";
    }
}
