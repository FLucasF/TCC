-- Support for three types of guests: individuals (CPF), companies (CNPJ), and foreigners (passport).
-- Exactly one set of identification fields must be present per reservation.

ALTER TABLE reservations ADD COLUMN company_cnpj text;
ALTER TABLE reservations ADD COLUMN company_name text;
ALTER TABLE reservations ADD COLUMN guest_passport text;
ALTER TABLE reservations ADD COLUMN guest_country text;

-- guest_cpf is no longer mandatory (one of the three ID sets is)
ALTER TABLE reservations ALTER COLUMN guest_cpf DROP NOT NULL;
