create table collections
(
    id            uuid default gen_random_uuid()
        constraint collections_pk
            primary key,
    user_id       uuid         not null
        constraint collections_users_id_fk
            references users,
    name          varchar(200) not null,
    protected_key varchar(200),
    created_at timestamptz default now() not null,
    updated_at timestamptz
);

--;;

alter table bookmarks
    add collection_id uuid,
    add constraint bookmarks_collections_id_fk
        foreign key (collection_id) references collections
            on delete set null;

--;;

-- Create default collections for all users
insert into collections (id, user_id, name, protected_key)
select gen_random_uuid() as id, id as user_id, 'Default' as name, null as protected_key
from users u;

--;;

-- Put all bookmarks into the default collection
update bookmarks
set collection_id = c.id
from collections c
where bookmarks.user_id = c.user_id