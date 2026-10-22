-- Allow reservations without CPF (for company or passport identification)
ALTER TABLE reservations ALTER COLUMN guest_cpf DROP NOT NULL;
