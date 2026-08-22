create table admins
(
    id            bigserial primary key,
    username      varchar(100) not null unique,
    password_hash varchar(100) not null,
    enabled       boolean      not null default true,
    created_at    timestamptz  not null default now()
);

create table persons
(
    id                 bigserial primary key,
    last_name          varchar(100) not null,
    first_name         varchar(100) not null,
    middle_name        varchar(100),
    card_id            varchar(100) not null unique,
    photo              bytea,
    photo_content_type varchar(100),
    active             boolean      not null default true,
    created_at         timestamptz  not null default now(),

    constraint chk_person_photo_size
        check (photo is null or octet_length(photo) <= 102400)
);

create table access_controllers
(
    id                  bigserial primary key,
    name                varchar(150) not null,
    ip                  varchar(100) not null unique,
    websocket_url       varchar(500),
    controller_password varchar(255),
    enabled             boolean      not null default true,
    connected           boolean      not null default false,
    authenticated       boolean      not null default false,
    last_seen           timestamptz,
    created_at          timestamptz  not null default now()
);

create table readers
(
    id                bigserial primary key,
    controller_id     bigint       not null
        references access_controllers (id) on delete cascade,
    reader_number     integer      not null,
    name              varchar(150) not null,
    reader_type       varchar(100) not null,
    port              integer      not null,
    exdev_number      integer      not null,
    exdev_direction   integer      not null,

    constraint uk_reader_controller_number
        unique (controller_id, reader_number),

    constraint chk_reader_number
        check (reader_number >= 0),

    constraint chk_reader_exdev_number
        check (exdev_number between 0 and 1),

    constraint chk_reader_exdev_direction
        check (exdev_direction between 0 and 1)
);

create table passage_events
(
    id                bigserial primary key,
    controller_id     bigint references access_controllers (id)
        on delete set null,
    person_id         bigint references persons (id)
        on delete set null,
    event_type        varchar(100) not null,
    card_id           varchar(100),
    device_number     integer,
    direction         integer,
    allowed           boolean      not null default false,
    remove_card       boolean,
    command_source    varchar(100),
    event_time        timestamptz  not null default now(),
    raw_json          text         not null
);

create index idx_passage_events_time
    on passage_events (event_time desc);

create index idx_passage_events_card
    on passage_events (card_id);

create index idx_passage_events_person
    on passage_events (person_id);