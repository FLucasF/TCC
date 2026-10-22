ALTER TABLE reservations
    ADD COLUMN guest_passport text,
    ADD COLUMN guest_country text,
    ADD COLUMN company_cnpj text,
    ADD COLUMN company_name text;

ALTER TABLE reservations
    ALTER COLUMN guest_cpf DROP NOT NULL;
