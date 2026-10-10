package loja.checkout.clube;

import org.springframework.stereotype.Component;

@Component
class Bronze implements NivelClube {
    @Override
    public String codigo() {
        return "BRONZE";
    }
}
