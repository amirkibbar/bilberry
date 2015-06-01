package ajk.gradle.elastic

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import static ajk.gradle.elastic.ElasticPlugin.CYAN
import static ajk.gradle.elastic.ElasticPlugin.NORMAL
import static ajk.gradle.elastic.ElasticPlugin.YELLOW
import static ajk.gradle.elastic.ElasticPlugin.getRED
import static org.apache.http.client.fluent.Executor.newInstance
import static org.apache.http.client.fluent.Request.Post

class StopElasticAction {
    @Input
    @Optional
    private Integer httpPort

    private AntBuilder ant

    StopElasticAction(Project project) {
        ant = project.ant
    }

    void execute() {
        println "${CYAN}* elastic:$NORMAL stopping ElasticSearch"

        try {
            newInstance().execute(Post("http://localhost:${httpPort ?: 9200}/_shutdown"))

            println "${CYAN}* elastic:$NORMAL waiting for ElasticSearch to shutdown"
            ant.waitfor(maxwait: 2, maxwaitunit: "minute", timeoutproperty: "elasticTimeout") {
                not {
                    ant.http(url: "http://localhost:$httpPort")
                }
            }

            if (ant.properties['elasticTimeout'] != null) {
                println "${RED}* elastic:$NORMAL could not stop ElasticSearch"
                throw new RuntimeException("failed to stop ElasticSearch")
            } else {
                println "${CYAN}* elastic:$NORMAL ElasticSearch is now down"
            }
        } catch (ConnectException e) {
            println "${CYAN}* elastic:$YELLOW warning - unable to stop elastic on http port ${httpPort ?: 9200}, ${e.message}$NORMAL"
        }
    }
}
