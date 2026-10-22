-- Support company bookings (CNPJ) and foreign guests (passport)
ALTER TABLE reservations ADD COLUMN company_cnpj text;
ALTER TABLE reservations ADD COLUMN company_name text;
ALTER TABLE reservations ADD COLUMN guest_passport text;
ALTER TABLE reservations ADD COLUMN guest_country text;

-- Make guest_cpf nullable since company/passport bookings don't have it
ALTER TABLE reservations ALTER COLUMN guest_cpf DROP NOT NULL;
