-- Corporate rate plan needs to track which company's discount is being used
ALTER TABLE reservations ADD COLUMN company_code text;
