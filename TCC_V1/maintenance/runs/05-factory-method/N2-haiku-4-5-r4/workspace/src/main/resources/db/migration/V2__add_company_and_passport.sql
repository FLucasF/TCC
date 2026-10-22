-- Support for company (CNPJ) and foreign guest (passport) identification
ALTER TABLE reservations
    ADD COLUMN guest_cnpj text,
    ADD COLUMN guest_passport text,
    ADD COLUMN guest_country text,
    ADD COLUMN company_name text;

-- Make guest_cpf optional by removing its NOT NULL constraint
ALTER TABLE reservations
    ALTER COLUMN guest_cpf DROP NOT NULL;
