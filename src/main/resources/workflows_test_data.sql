insert into wf_workflow (id, name, version, version_hash)
    values
    (1, 'test1', '1.0', 'aaa');
insert into wf_node (id, workflow_id, name, class)
    values
    (1, 1, 'trigger', 'AsyncUrl'),
    (2, 1, 'worker ackq', 'AsyncUrl'),
    (3, 1, 'hiring ackq', 'AsyncUrl'),
    (4, 1, 'worker', 'AsyncUrl'),
    (5, 1, 'hiring', 'AsyncUrl'),
    (6, 1, 'st1', 'AsyncUrl'),
    (7, 1, 'st2', 'AsyncUrl'),
    (8, 1, 'publish', 'AsyncUrl'),
    (9, 1, 'mlde', 'AsyncUrl');
insert into wf_edge (workflow_id, source_step_id, target_step_id)
    values
    (1, 1, 2),
    (1, 1, 3),
    (1, 2, 4),
    (1, 3, 5),
    (1, 4, 6),
    (1, 4, 7),
    (1, 6, 8),
    (1, 7, 8),
    (1, 8, 9),
    (1, 5, 9);
insert into wf_workflow_instance (id, workflow_id, workflow_status) values (1, 1, 'Y');
insert into wf_node_instance (node_id, workflow_instance_id, node_status)
    values
    (1, 1, 'Y'),
    (2, 1, 'Y'),
    (3, 1, 'Y'),
    (4, 1, 'Y'),
    (5, 1, 'Y'),
    (6, 1, 'Y'),
    (7, 1, 'Y'),
    (8, 1, 'Y'),
    (9, 1, 'Y');