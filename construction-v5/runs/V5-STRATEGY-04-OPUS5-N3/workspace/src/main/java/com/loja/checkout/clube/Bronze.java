package com.loja.checkout.clube;

/** Bronze: só o cadastro, não ganha nada. */
class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
