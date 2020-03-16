package cz.yarpen.workflows

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class WorkflowsWorker(
    @Autowired
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {

    @Scheduled(fixedRate = 1000)
    @Transactional
    fun processQueue() {
        val cutoffDate = LocalDateTime.now()
        val args: MapSqlParameterSource = MapSqlParameterSource()
        args.addValue("cutoff", cutoffDate)
        val jobRow = jdbcTemplate.queryForList(
            """select * from wf_execution_queue
            where next_trigger <= :cutoff or next_trigger is null
            order by priority desc
            limit 1
            for update
        """, args
        )

        if (jobRow.size != 1)  {
            return
        }

        val queueId = jobRow[0]["id"] as Number
        val nodeInstanceId = jobRow[0]["node_instance_id"] as Number

        processNodeInstance(nodeInstanceId)

        jdbcTemplate.update(
            "delete from wf_execution_queue where id = :queue_id",
            mapOf<String, Number>("queue_id" to queueId)
        )

    }

    private fun processNodeInstance(nodeInstanceId: Number) {
        // TODO/FIXME: real processing

        // all goes well, complete. TODO/FIXME: state machine for step
        jdbcTemplate.update(
            """update wf_node_instance
            set node_status = 'C' where id = :node_instance_id""",
            mapOf<String, Number>("node_instance_id" to nodeInstanceId)
        )

        // queue child nodes
        // 1) find all children, 2) check child parents
        val nodesToQueue = jdbcTemplate.queryForList(
            """select wni_parents.*, wni_children.id as node_instance_id_to_queue
    from
          wf_node_instance wni_complete
        , wf_edge we -- complete to children
        , wf_edge we_sib -- find siblings by target
        , wf_node_instance wni_parents
        , wf_node_instance wni_children
    where 1 = 1
        and wni_complete.id = :node_instance_id -- input
        and we.source_step_id = wni_complete.node_id
        and we_sib.target_step_id = we.target_step_id
        and wni_parents.node_id = we_sib.source_step_id
        and wni_parents.workflow_instance_id = wni_complete.workflow_instance_id
        and wni_children.node_id = we_sib.target_step_id
        and wni_children.workflow_instance_id = wni_complete.workflow_instance_id

        """, mapOf<String, Number>("node_instance_id" to nodeInstanceId)
        )

        var doInsert = true
        val realQueuedNodes = mutableSetOf<Number>()
        //jdbcTemplate.update("savepoint nodes_queueing", mapOf<String,Any>())
        nodesToQueue.forEach {
            if (it["node_status"] == "C") {
                realQueuedNodes.add(it["node_instance_id_to_queue"] as Number)
            }
            else {
                doInsert = false
            }
        }

        if (doInsert) {
            realQueuedNodes.forEach {
                jdbcTemplate.update("""insert into wf_execution_queue (node_instance_id, priority, next_trigger)
                    values (:node_instance_id, :priority, :next_trigger)
                """, mapOf<String,Any?>(
                    "node_instance_id" to it,
                    "priority" to 100, // TODO/FIXME: get wfi prio
                    "next_trigger" to null // TODO/FIXME: next val for async nodes
                ))
            }
        }

    }
}