-- the company agreement (CORPORATE rate plan) a reservation was made under; empty for the other plans
ALTER TABLE reservations ADD COLUMN company_code text;
