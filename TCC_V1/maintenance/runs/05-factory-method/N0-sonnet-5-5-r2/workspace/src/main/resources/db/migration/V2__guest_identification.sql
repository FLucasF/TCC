-- A guest is identified by a CPF, or is a company's guest (CNPJ and company name), or is a foreigner
-- (passport and country). Reservations made so far all have a CPF.
ALTER TABLE reservations
    ALTER COLUMN guest_cpf DROP NOT NULL,
    ADD COLUMN company_cnpj  text,
    ADD COLUMN company_name  text,
    ADD COLUMN guest_passport text,
    ADD COLUMN guest_country text,
    ADD CONSTRAINT reservations_one_identification CHECK (
        (guest_cpf IS NOT NULL AND company_cnpj IS NULL AND company_name IS NULL
            AND guest_passport IS NULL AND guest_country IS NULL)
        OR (guest_cpf IS NULL AND company_cnpj IS NOT NULL AND company_name IS NOT NULL
            AND guest_passport IS NULL AND guest_country IS NULL)
        OR (guest_cpf IS NULL AND company_cnpj IS NULL AND company_name IS NULL
            AND guest_passport IS NOT NULL AND guest_country IS NOT NULL));
