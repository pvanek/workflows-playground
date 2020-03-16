select * from wf_workflow ;

select * from wf_node ;

select * from wf_edge ;

select * from wf_workflow_instance ;

select * from wf_node_instance;

select * from wf_execution_queue weq ;

-- find the parent
select distinct e1.source_step_id
    from wf_edge e1
        left outer join wf_edge e2 on (e1.source_step_id = e2.target_step_id)
    where e2.source_step_id is null
        and e1.workflow_id = 1;

select * from wf_status ;

select node_status, count(1) as node_status_count
    from wf_node_instance 
    where workflow_instance_id = 1
        and node_id = 1
    group by node_status;
select *
    from wf_node_instance 
    where workflow_instance_id = 1
        and node_id = 1;
        
select * from wf_edge we2;

select * from wf_execution_queue weq where weq.id = 1;
select * from wf_node_instance wni where wni.id = 1; -- weq.node_instance_id
select * from wf_edge we where we.source_step_id = 1; -- wni.node_id
select * from wf_edge we1 where we1.target_step_id = 2; -- we.target_step_id
select wni1.* from wf_node_instance wni1
    where wni1.node_id = 1
    and wni1.workflow_instance_id = 1 -- we1.worklfow_instance_id
    ; -- we1.source_step_id

select * from wf_node_instance;
select wni_parents.*, wni_parents.id
    from
          wf_node_instance wni_complete
        , wf_edge we -- complete to children
        , wf_edge we_sib -- find siblings by target
        , wf_node_instance wni_parents
    where 1 = 1
        and wni_complete.id = 16 -- input
        and we.source_step_id = wni_complete.node_id
        and we_sib.target_step_id = we.target_step_id
        and wni_parents.node_id = we_sib.source_step_id
        and wni_parents.workflow_instance_id = wni_complete.workflow_instance_id
;


select wni.id as root_node_instance_id
    from wf_node_instance wni
    where 1 = 1
        and wni.workflow_instance_id = 4
        and exists (
            select 1
                from wf_edge e1
                left outer join wf_edge e2 on
                    (e1.source_step_id = e2.target_step_id)
                where e2.source_step_id is null
                    and e1.workflow_id = 1
                    and e1.source_step_id = wni.node_id 
        )
;