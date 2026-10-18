package com.loja.api;

import io.javalin.Javalin;
import com.loja.model.*;
import com.loja.repository.CustomerRepository;

public class CustomerEndpoint {
    private final CustomerRepository customerRepo;

    public CustomerEndpoint(CustomerRepository customerRepo) {
        this.customerRepo = customerRepo;
    }

    public void register(Javalin app) {
        app.get("/customers/{cpf}", ctx -> {
            Customer customer = customerRepo.findByCpf(ctx.pathParam("cpf"));
            if (customer == null) {
                ctx.status(404).json(new ErrorResponse("CUSTOMER_NOT_FOUND"));
                return;
            }
            ctx.status(200).json(customer);
        });
    }
}
