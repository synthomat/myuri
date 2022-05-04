-- auto-generated definition
create table bookmarks
(
    id         bigserial
        constraint bookmarks_pk
            primary key,
    site_url   varchar(500) not null,
    site_title varchar(500),
    is_deleted bool default false,
    created_at timestamp with time zone default now()
);
