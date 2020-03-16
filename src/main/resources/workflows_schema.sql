drop table if exists wf_workflow;
create table wf_workflow
(
    id bigint not null auto_increment primary key,
    name varchar(36) not null,
    version varchar(36) not null,
    version_hash char(128) not null, -- sha512
    description varchar(255) null,
    -- audit columns
    created_date datetime not null default now(),
    updated_date datetime null
);
--
drop table if exists wf_node;
create table wf_node
(
    id bigint not null auto_increment primary key,
    workflow_id bigint not null,
    name varchar(36) not null,
    description varchar(255) null,
    class varchar(36) not null,
    -- audit columns
    created_date datetime not null default now(),
    updated_date datetime null,
    --
    index wf_steps_workflow_id_ix (workflow_id),
    foreign key (workflow_id) references wf_workflow(id)
);
--
drop table if exists wf_edge;
create table wf_edge
(
    id bigint not null auto_increment primary key,
    workflow_id bigint not null,
    source_step_id bigint not null,
    target_step_id bigint not null,
    -- audit columns
    created_date datetime not null default now(),
    updated_date datetime null
    -- TODO/FIXME: indexes
);
--
drop table if exists wf_status;
create table wf_status
(
    status_code char(1) not null primary key,
    description varchar(15) not null
);
insert into wf_status (status_code, description)
    values
    ('Y', 'Ready'),
    ('I', 'In progress'),
    ('R', 'Retry'),
    ('C', 'Complete'),
    ('E', 'Error'),
    ('W', 'Waiting'),
    ('X', 'Canceled'),
    ('S', 'Skipped')
;
--
drop table if exists wf_workflow_instance;
create table wf_workflow_instance
(
    id bigint not null auto_increment primary key,
    workflow_id bigint not null,
    workflow_status char(1) not null default 'Y', -- TODO/FIXME: keep the denormalization?
    workflow_data text null,
    priority tinyint unsigned not null default 100,
    -- audit columns
    created_date datetime not null default now(),
    updated_date datetime null
);
--
drop table if exists wf_node_instance;
create table wf_node_instance
(
    id bigint not null auto_increment primary key,
    workflow_instance_id bigint not null,
    node_id bigint not null,
    node_status char(1) not null default 'Y',
    next_trigger datetime,
    -- audit columns
    created_date datetime not null default now(),
    updated_date datetime null
);
--
drop table if exists wf_execution_queue;
create table wf_execution_queue
(
    id bigint not null auto_increment primary key,
    node_instance_id bigint not null,
    priority tinyint unsigned not null default 100,
    next_trigger datetime,
    -- audit columns
    created_date datetime not null default now(),
    updated_date datetime null
);