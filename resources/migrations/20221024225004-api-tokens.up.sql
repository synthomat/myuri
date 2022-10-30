create table api_tokens
(
    id          uuid        default gen_random_uuid() not null
        constraint api_tokens_pk
            primary key,
    token       varchar(50)                           not null,
    name        varchar(100),
    valid_until timestamptz                           not null,
    disabled    boolean     default false             not null,
    user_id     uuid                                  not null
        constraint api_tokens_users_id_fk
            references users (id)
            on delete cascade,
    created_at  timestamptz default now()             not null
);