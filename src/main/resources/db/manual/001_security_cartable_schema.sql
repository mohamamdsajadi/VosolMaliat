create table if not exists app_user_profile (
  id integer primary key,
  keycloak_user_id varchar(255) not null unique,
  username varchar(255) not null unique,
  email varchar(255) unique,
  first_name varchar(255),
  last_name varchar(255),
  phone_number varchar(255),
  enabled_in_business_system boolean not null,
  locked_in_business_system boolean not null,
  created_at timestamp,
  updated_at timestamp,
  created_by varchar(255),
  updated_by varchar(255),
  deleted_at timestamp,
  deleted_by varchar(255),
  is_deleted boolean
);

create table if not exists permission (
  id integer primary key,
  code varchar(255) not null unique,
  description varchar(255),
  enabled boolean not null,
  created_at timestamp,
  updated_at timestamp,
  created_by varchar(255),
  updated_by varchar(255),
  deleted_at timestamp,
  deleted_by varchar(255),
  is_deleted boolean
);

create table if not exists group_permission (
  id integer primary key,
  group_name varchar(255) not null,
  permission_id integer not null,
  created_at timestamp,
  updated_at timestamp,
  created_by varchar(255),
  updated_by varchar(255),
  deleted_at timestamp,
  deleted_by varchar(255),
  is_deleted boolean,
  unique (group_name, permission_id),
  foreign key (permission_id) references permission(id)
);

create table if not exists audit_log (
  id integer primary key,
  actor_username varchar(255),
  actor_keycloak_user_id varchar(255),
  action varchar(255) not null,
  target_type varchar(255) not null,
  target_id varchar(255),
  old_value varchar(2000),
  new_value varchar(2000),
  created_at timestamp not null
);
