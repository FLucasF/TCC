ALTER TABLE reservations
    ADD COLUMN guest_cpf_alt TEXT,
    ADD COLUMN company_cnpj TEXT,
    ADD COLUMN company_name TEXT,
    ADD COLUMN guest_passport TEXT,
    ADD COLUMN guest_country TEXT;

ALTER TABLE reservations
    ALTER COLUMN guest_cpf DROP NOT NULL;
