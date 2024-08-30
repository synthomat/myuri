alter table user_settings
    drop column target_blank;
--;;
alter table user_settings
    add setting_name varchar(50) not null,
    add json_value json not null;
--;;
alter table user_settings
    add constraint user_settings_pk
        unique (user_id, setting_name);