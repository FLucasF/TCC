-- the company code of a corporate-plan reservation; null for the other plans
ALTER TABLE reservations ADD COLUMN company_code text;
