package ajk.gradle.elastic

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import static org.apache.http.client.fluent.Request.Get

import static org.apache.http.client.fluent.Executor.newInstance
import org.gradle.api.Project

class StopElasticAction {
    @Input
    @Optional
    private Integer httpPort

    private AntBuilder ant

    StopElasticAction(Project project) {
        ant = project.ant
    }

    void execute() {
        println "* elastic: stopping ElasticSearch"

        newInstance().execute(Get("http://localhost:${httpPort ?: 9200}/_shutdown"))
    }
}
