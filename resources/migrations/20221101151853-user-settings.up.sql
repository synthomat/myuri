create table user_settings
(
    user_id      uuid        not null
        constraint user_settings_users_null_fk
            references users
            on delete cascade,
    setting_name varchar(50) not null,
    json_value   json        not null,
    constraint user_settings_pk
        primary key (user_id, setting_name)
);