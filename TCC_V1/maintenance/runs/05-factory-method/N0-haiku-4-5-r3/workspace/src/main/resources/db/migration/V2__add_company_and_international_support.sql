-- Add new columns for company (CNPJ) and international (passport) support
ALTER TABLE reservations ADD COLUMN company_cnpj text;
ALTER TABLE reservations ADD COLUMN company_name text;
ALTER TABLE reservations ADD COLUMN guest_passport text;
ALTER TABLE reservations ADD COLUMN guest_country text;

-- guest_cpf is no longer required; validation happens at application level
ALTER TABLE reservations ALTER COLUMN guest_cpf DROP NOT NULL;
