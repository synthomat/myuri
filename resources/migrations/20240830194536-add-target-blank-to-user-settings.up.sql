alter table user_settings
    drop column setting_name,
    drop column json_value;
--;;
alter table user_settings
    add target_blank boolean default true not null;


