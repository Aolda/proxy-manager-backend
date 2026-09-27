-- MySQL/MariaDB: apply once before deploying the new backend if Hibernate
-- schema update has not already created this column. Existing rows remain off.
ALTER TABLE forwarding ADD COLUMN proxy_protocol BOOLEAN DEFAULT FALSE;
