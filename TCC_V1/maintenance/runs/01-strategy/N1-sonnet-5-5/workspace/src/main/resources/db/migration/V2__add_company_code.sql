-- the company code the guest gave, for the corporate rate plan; null for the other plans
ALTER TABLE reservations ADD COLUMN company_code text;
