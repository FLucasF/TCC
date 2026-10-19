package com.pousadamareaalta.exception;

public class OperationNotAllowed extends ApiException {
    public OperationNotAllowed() {
        super("OPERATION_NOT_ALLOWED", "Operation not allowed in current reservation state", 409);
    }
}
