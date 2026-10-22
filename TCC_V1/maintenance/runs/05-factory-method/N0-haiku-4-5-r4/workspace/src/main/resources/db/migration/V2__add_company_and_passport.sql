-- Add new columns for company and passport identification
ALTER TABLE reservations ADD COLUMN guest_cnpj text;
ALTER TABLE reservations ADD COLUMN company_name text;
ALTER TABLE reservations ADD COLUMN guest_passport text;
ALTER TABLE reservations ADD COLUMN guest_country text;
