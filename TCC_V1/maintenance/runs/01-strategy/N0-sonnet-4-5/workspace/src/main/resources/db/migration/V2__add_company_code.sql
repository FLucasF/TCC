-- Add company_code column for CORPORATE rate plan
ALTER TABLE reservations ADD COLUMN company_code text;
