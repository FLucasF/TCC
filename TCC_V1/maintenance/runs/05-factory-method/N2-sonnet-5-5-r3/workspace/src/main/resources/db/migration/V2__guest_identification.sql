-- A guest is identified by CPF, by company (CNPJ and name) or by passport (number and country).
-- type says which; number is the CPF, CNPJ or passport number; detail is the company name or the country.
ALTER TABLE reservations
    ADD COLUMN identification_type   text,
    ADD COLUMN identification_number text,
    ADD COLUMN identification_detail text;

UPDATE reservations SET identification_type = 'CPF', identification_number = guest_cpf;

ALTER TABLE reservations
    ALTER COLUMN identification_type SET NOT NULL,
    ALTER COLUMN identification_number SET NOT NULL,
    DROP COLUMN guest_cpf;
