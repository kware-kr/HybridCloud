```sh
k exec postgres-postgresql-0 -it -- /bin/bash
psql -U postgres # 1234
```

```sql
create table counter  
(  
    id           bigint generated always as identity  
        constraint counter_pk  
            primary key,  
    ip           text,  
    date_created timestamp with time zone default now()  
);  
  
grant insert on counter to lect;
grant select on counter to lect;
```

```sh
\q
exit
```