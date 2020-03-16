package cz.yarpen.workflows

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


data class Workflow(
    val name: String,
    val version: String,
    val description: String,
    val nodes: List<WorkflowNode>,
    val edges: List<WorkflowEdge>
)

data class WorkflowNode(
    val name: String,
    val description: String?,
    val className: String
)

data class WorkflowEdge(
    val from_node: String,
    val to_node: String
)

data class WorkflowPostResponse(
    val workflowId: Int
)

data class WorkflowInstance(
    val priority: Int,
    val data: Map<String, Any>?
)

data class WorkflowInstancePostResponse(
    val workflowInstanceId: Int
)


@RestController
@RequestMapping("/workflows")
class GreetingController(
    @Autowired
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {

    @PostMapping(
        "/workflows",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Transactional
    fun createWorkflow(@RequestBody body: Workflow): WorkflowPostResponse {
        val keyHolder: KeyHolder = GeneratedKeyHolder()
        val args: MapSqlParameterSource = MapSqlParameterSource()
        args.addValue("name", body.name)
            .addValue("version", body.version)
            .addValue("version_hash", "too/fixme")
            .addValue("description", body.description)
        jdbcTemplate.update(
            """insert into wf_workflow (name, version, version_hash, description)
            values (:name, :version, :version_hash, :description)
        """, args, keyHolder
        )
        val workflowId = keyHolder.key!!.toInt()

        val nodeMap = mutableMapOf<String, Int>()
        body.nodes.forEach { node: WorkflowNode ->
            val nodeArgs: MapSqlParameterSource = MapSqlParameterSource()
            nodeArgs.addValue("workflow_id", workflowId)
                .addValue("name", node.name)
                .addValue("description", node.description)
                .addValue("class", node.className)
            jdbcTemplate.update(
                """insert into wf_node (workflow_id, name, description, class)
                values (:workflow_id, :name, :description, :class)
            """, nodeArgs, keyHolder
            )
            nodeMap[node.name] = keyHolder.key!!.toInt()
        }

        body.edges.forEach { edge: WorkflowEdge ->
            val edgeArgs: MapSqlParameterSource = MapSqlParameterSource()
            edgeArgs.addValue("workflow_id", workflowId)
                .addValue("source_step_id", nodeMap[edge.from_node])
                .addValue("target_step_id", nodeMap[edge.to_node])
            jdbcTemplate.update(
                """insert into wf_edge (workflow_id, source_step_id, target_step_id)
               values (:workflow_id, :source_step_id, :target_step_id)
            """, edgeArgs
            )
        }

        return WorkflowPostResponse(workflowId = workflowId)
    }

    @GetMapping("/workflows")
    fun getWorkflows(): List<Workflow> {
        return listOf<Workflow>()
    }

    @PostMapping(
        "/instances/{workflowId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Transactional
    fun createWorkflowInstance(
        @PathVariable workflowId: Int,
        @RequestBody body: WorkflowInstance
    ): WorkflowInstancePostResponse {
        val keyHolder: KeyHolder = GeneratedKeyHolder()

        val args: MapSqlParameterSource = MapSqlParameterSource()
        args.addValue("priority", body.priority)
            .addValue("workflow_data", body.data.toString())
            .addValue("workflow_id", workflowId)
        jdbcTemplate.update(
            """insert into wf_workflow_instance (workflow_id, priority, workflow_data)
            values (:workflow_id, :priority, :workflow_data)
        """, args, keyHolder
        )
        val workflowInstanceId = keyHolder.key!!.toInt()
        args.addValue("workflow_instance_id", workflowInstanceId)

        // node instances
        val argsNodesIntance: MapSqlParameterSource = MapSqlParameterSource()
        argsNodesIntance.addValue("workflow_instance_id", workflowInstanceId)
            .addValue("workflow_id", workflowId)
        jdbcTemplate.update(
            """insert into wf_node_instance(workflow_instance_id, node_id)
            select :workflow_instance_id, id
                from wf_node
                where workflow_id = :workflow_id""", argsNodesIntance
        )

        // find parent nodes to queue them
        val nodesToQueue = jdbcTemplate.queryForList(
            """select wni.id as root_node_instance_id
    from wf_node_instance wni
    where 1 = 1
        and wni.workflow_instance_id = :workflow_instance_id
        and exists (
            select 1
                from wf_edge e1
                left outer join wf_edge e2 on
                    (e1.source_step_id = e2.target_step_id)
                where e2.source_step_id is null
                    and e1.workflow_id = :workflow_id
                    and e1.source_step_id = wni.node_id
        )"""
            , args
        )
        nodesToQueue.forEach {
            val nodeToQueue = it["root_node_instance_id"]!! as Number
            val queueArgs = MapSqlParameterSource()
            queueArgs.addValue("node_instance_id", nodeToQueue)
                .addValue("priority", body.priority)
            jdbcTemplate.update(
                """insert into wf_execution_queue (node_instance_id, priority)
                values (:node_instance_id, :priority)
            """, queueArgs, keyHolder
            )
            println("Queued step id: $nodeToQueue as ID: ${keyHolder.key!!}")
        }

        return WorkflowInstancePostResponse(
            workflowInstanceId = workflowInstanceId
        )

    }

}