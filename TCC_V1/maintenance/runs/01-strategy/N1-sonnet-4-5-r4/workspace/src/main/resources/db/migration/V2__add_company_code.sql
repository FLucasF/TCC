-- Adds the company_code column for the CORPORATE rate plan.
ALTER TABLE reservations ADD COLUMN company_code text;
