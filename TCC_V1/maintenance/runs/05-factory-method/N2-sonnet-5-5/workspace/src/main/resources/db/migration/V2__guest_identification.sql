-- A guest is identified by CPF, or by a company (CNPJ and name), or by passport and country.
ALTER TABLE reservations
    ALTER COLUMN guest_cpf DROP NOT NULL,
    ADD COLUMN company_cnpj    text,
    ADD COLUMN company_name    text,
    ADD COLUMN guest_passport  text,
    ADD COLUMN guest_country   text;
