create table collections
(
    id            uuid default gen_random_uuid()
        constraint collections_pk
            primary key,
    user_id       uuid         not null
        constraint collections_users_id_fk
            references users,
    name          varchar(200) not null,
    protected_key varchar(200)
);

--;;


alter table bookmarks
    add collection_id uuid,
    add constraint bookmarks_collections_id_fk
        foreign key (collection_id) references collections;