-- A guest is identified by a CPF, by a company (CNPJ + legal name) or by a passport (+ country).
ALTER TABLE reservations
    ALTER COLUMN guest_cpf DROP NOT NULL,
    ADD COLUMN company_cnpj  text,
    ADD COLUMN company_name  text,
    ADD COLUMN guest_passport text,
    ADD COLUMN guest_country text;
