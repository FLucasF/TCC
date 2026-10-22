-- Support for three types of guest identification: CPF (Brazilian individual), CNPJ (company), and passport (foreigner)

ALTER TABLE reservations
    ALTER COLUMN guest_cpf DROP NOT NULL,
    ADD COLUMN company_cnpj text,
    ADD COLUMN company_name text,
    ADD COLUMN guest_passport text,
    ADD COLUMN guest_country text,
    ADD CONSTRAINT guest_identification_type CHECK (
        (guest_cpf IS NOT NULL AND company_cnpj IS NULL AND company_name IS NULL AND guest_passport IS NULL AND guest_country IS NULL) OR
        (guest_cpf IS NULL AND company_cnpj IS NOT NULL AND company_name IS NOT NULL AND guest_passport IS NULL AND guest_country IS NULL) OR
        (guest_cpf IS NULL AND company_cnpj IS NULL AND company_name IS NULL AND guest_passport IS NOT NULL AND guest_country IS NOT NULL)
    );
