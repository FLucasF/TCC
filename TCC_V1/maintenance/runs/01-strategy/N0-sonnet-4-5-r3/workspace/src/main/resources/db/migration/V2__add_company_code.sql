-- Add company_code column to support corporate rate plan
ALTER TABLE reservations ADD COLUMN company_code text;
