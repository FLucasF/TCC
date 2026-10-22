-- Support for companies (CNPJ) and foreign guests (passport)
ALTER TABLE reservations ALTER COLUMN guest_cpf DROP NOT NULL;
ALTER TABLE reservations ADD COLUMN company_cnpj text;
ALTER TABLE reservations ADD COLUMN company_name text;
ALTER TABLE reservations ADD COLUMN guest_passport text;
ALTER TABLE reservations ADD COLUMN guest_country text;

-- Exactly one identification type is required
ALTER TABLE reservations ADD CONSTRAINT check_one_identification_type CHECK (
    (guest_cpf IS NOT NULL AND company_cnpj IS NULL AND guest_passport IS NULL)
    OR (company_cnpj IS NOT NULL AND company_name IS NOT NULL AND guest_cpf IS NULL AND guest_passport IS NULL)
    OR (guest_passport IS NOT NULL AND guest_country IS NOT NULL AND guest_cpf IS NULL AND company_cnpj IS NULL)
);
