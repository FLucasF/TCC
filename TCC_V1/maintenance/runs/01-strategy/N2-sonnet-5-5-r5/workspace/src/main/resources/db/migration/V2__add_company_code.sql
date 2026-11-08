-- the company code of a CORPORATE reservation; the other rate plans leave it empty
ALTER TABLE reservations ADD COLUMN company_code text;
