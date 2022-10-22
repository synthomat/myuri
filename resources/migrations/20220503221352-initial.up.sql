create table users
(
    id                uuid                     default gen_random_uuid() not null
        constraint users_pk
            primary key,
    username          varchar(20)                                        not null,
    email             varchar(100)                                       not null,
    password_digest   varchar(120)                                       not null,
    created_at        timestamp with time zone default now()             not null,
    verification_code uuid
);
--;;
create table bookmarks
(
    id         uuid                     default gen_random_uuid() not null
        constraint bookmarks_pk
            primary key,
    site_url   varchar(500)                                       not null,
    site_title varchar(500),
    created_at timestamp with time zone default now(),
    is_deleted boolean                  default false,
    user_id    uuid
        constraint users_fk
            references users
            on delete cascade
);
--;;
create unique index users_email_uindex
    on users (email);
--;;
create unique index users_username_uindex
    on users (username);

