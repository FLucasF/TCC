-- the company code of a corporate-rate reservation; null for the other rate plans
ALTER TABLE reservations ADD COLUMN company_code text;
