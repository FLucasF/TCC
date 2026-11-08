package br.com.marealta;

import br.com.marealta.store.MemoryStore;
import br.com.marealta.store.Store;

class MemoryApiTest extends ApiTestBase {
    @Override
    Store newStore() {
        return new MemoryStore(Config.ROOMS);
    }
}
