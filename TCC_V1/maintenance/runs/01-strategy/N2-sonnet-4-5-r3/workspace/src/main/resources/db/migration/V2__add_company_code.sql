-- Add company_code field for corporate rate plan
ALTER TABLE reservations ADD COLUMN company_code text;
