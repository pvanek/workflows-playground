package cz.yarpen.workflows

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource


@SpringBootApplication
@EnableScheduling
class WorkflowsApplication

fun main(args: Array<String>) {
    runApplication<WorkflowsApplication>(*args)
}


@Configuration
class DbConfiguration {

    @Bean
    fun mysqlDataSource(): DataSource {
        return DataSourceBuilder.create()
            .driverClassName("com.mysql.cj.jdbc.Driver")
            .url("jdbc:mysql://localhost:3306/workflows")
            .username("root")
            .password("")
            .build();
    }
}
