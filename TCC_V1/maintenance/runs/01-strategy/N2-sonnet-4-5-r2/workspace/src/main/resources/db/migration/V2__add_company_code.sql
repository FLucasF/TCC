-- Corporate rate plan: some reservations come from a partnered company.
ALTER TABLE reservations ADD COLUMN company_code text;
