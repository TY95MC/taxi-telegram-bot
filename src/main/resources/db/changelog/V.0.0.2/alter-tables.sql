--liquibase formatted sql
--changeset TY95MC:alter-tables

alter table cars drop column driver_id;

create table if not exists drivers_cars(
    car_id BIGINT references cars(id) on delete cascade,
    driver_id BIGINT references employees(id) on delete set null
);

