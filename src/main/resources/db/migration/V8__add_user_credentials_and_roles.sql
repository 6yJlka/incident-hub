alter table users
    add column password_hash varchar(100),
    add column role varchar(20);

update users
set role = 'REPORTER';

alter table users
    alter column role set not null;

alter table users
    add constraint chk_users_role
        check (role in ('REPORTER', 'ENGINEER', 'ADMIN'));
