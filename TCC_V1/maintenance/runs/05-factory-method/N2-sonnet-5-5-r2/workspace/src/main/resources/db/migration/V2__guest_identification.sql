-- A guest is identified by CPF, or by a company's CNPJ and name, or by passport and country.
ALTER TABLE reservations ALTER COLUMN guest_cpf DROP NOT NULL;
ALTER TABLE reservations ADD COLUMN company_cnpj text;
ALTER TABLE reservations ADD COLUMN company_name text;
ALTER TABLE reservations ADD COLUMN guest_passport text;
ALTER TABLE reservations ADD COLUMN guest_country text;
