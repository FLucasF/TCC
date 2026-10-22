-- New identity fields: corporate and foreign guests
ALTER TABLE reservations ADD COLUMN company_cnpj text;
ALTER TABLE reservations ADD COLUMN company_name text;
ALTER TABLE reservations ADD COLUMN guest_passport text;
ALTER TABLE reservations ADD COLUMN guest_country text;

-- guest_cpf is no longer always required (it can be null for corporate and foreign guests)
ALTER TABLE reservations ALTER COLUMN guest_cpf DROP NOT NULL;

-- Ensure exactly one identity type is present
ALTER TABLE reservations ADD CONSTRAINT reservations_one_identity
    CHECK (
        (guest_cpf IS NOT NULL AND company_cnpj IS NULL AND guest_passport IS NULL) OR
        (guest_cpf IS NULL AND company_cnpj IS NOT NULL AND company_name IS NOT NULL AND guest_passport IS NULL) OR
        (guest_cpf IS NULL AND company_cnpj IS NULL AND guest_passport IS NOT NULL AND guest_country IS NOT NULL)
    );
