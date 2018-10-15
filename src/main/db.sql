
create table groups
(
  id   int auto_increment
    primary key,
  name varchar(45) not null
);

create table users
(
  id    int auto_increment
    primary key,
  login varchar(45) not null
);

create table users_groups
(
  user_id  int null,
  group_id int null,
  constraint users_groups_ibfk_1
  foreign key (user_id) references users (id)
    on delete cascade,
  constraint users_groups_ibfk_2
  foreign key (group_id) references groups (id)
    on delete cascade
);

create index group_id
  on users_groups (group_id);

create index user_id
  on users_groups (user_id);