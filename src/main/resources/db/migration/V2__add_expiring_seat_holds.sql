ALTER TABLE bookings ADD COLUMN hold_expires_at TIMESTAMP;
ALTER TABLE show_seats ADD COLUMN owner_booking_id BIGINT;

ALTER TABLE show_seats
  ADD CONSTRAINT fk_show_seats_owner_booking
  FOREIGN KEY (owner_booking_id) REFERENCES bookings(id) ON DELETE SET NULL;

CREATE INDEX idx_bookings_expiry ON bookings(status, hold_expires_at, id);
CREATE INDEX idx_show_seats_owner_booking ON show_seats(owner_booking_id);

UPDATE show_seats ss
SET owner_booking_id = (
  SELECT MAX(bss.booking_id)
  FROM booking_show_seats bss
  JOIN bookings b ON b.id = bss.booking_id
  WHERE bss.show_seat_id = ss.id
    AND ((ss.status = 'LOCKED' AND b.status IN ('PROCESSING', 'PAYMENT_INITIATED'))
      OR (ss.status = 'BOOKED' AND b.status = 'COMPLETED'))
)
WHERE ss.status IN ('LOCKED', 'BOOKED');

UPDATE bookings
SET hold_expires_at = CURRENT_TIMESTAMP
WHERE status IN ('PROCESSING', 'PAYMENT_INITIATED')
  AND hold_expires_at IS NULL;

UPDATE show_seats
SET status = 'AVAILABLE', version = version + 1
WHERE status = 'LOCKED' AND owner_booking_id IS NULL;
