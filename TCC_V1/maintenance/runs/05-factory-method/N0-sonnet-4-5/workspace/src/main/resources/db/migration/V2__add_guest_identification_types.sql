-- Support for different guest identification types: CPF, company (CNPJ), or foreign passport

ALTER TABLE reservations
    ALTER COLUMN guest_cpf DROP NOT NULL;

ALTER TABLE reservations
    ADD COLUMN company_cnpj text,
    ADD COLUMN company_name text,
    ADD COLUMN guest_passport text,
    ADD COLUMN guest_country text;

-- Exactly one type of identification must be present
ALTER TABLE reservations
    ADD CONSTRAINT reservations_one_identification_type CHECK (
        (guest_cpf IS NOT NULL AND company_cnpj IS NULL AND guest_passport IS NULL) OR
        (guest_cpf IS NULL AND company_cnpj IS NOT NULL AND company_name IS NOT NULL AND guest_passport IS NULL) OR
        (guest_cpf IS NULL AND company_cnpj IS NULL AND guest_passport IS NOT NULL AND guest_country IS NOT NULL)
    );
