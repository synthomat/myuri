alter table users
    add is_admin bool default false not null;
--;;
-- Make oldest user admin!
update users
    set is_admin = true
    where id = (select id from users order by created_at limit 1)
