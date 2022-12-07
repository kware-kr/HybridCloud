create table public.counter
(
    id           bigint generated always as identity
        constraint counter_pk
            primary key,
    ip           text,
    date_created timestamp with time zone default now()
);

alter table public.counter
    owner to postgres;

grant insert, select on public.counter to lect;