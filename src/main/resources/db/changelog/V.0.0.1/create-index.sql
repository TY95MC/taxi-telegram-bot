--liquibase formatted sql
--changeset TY95MC:create-index

create index IF NOT EXISTS license_plate_index ON cars(license_plate);