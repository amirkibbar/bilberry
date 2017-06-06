package ajk.gradle.elastic

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import static ajk.gradle.elastic.ElasticPlugin.*
import static org.apache.http.client.fluent.Executor.newInstance
import static org.apache.http.client.fluent.Request.Post

class StopElasticAction {

    @Input
    @Optional
    private Integer httpPort

    @Input
    @Optional
    File toolsDir

    @Input
    @Optional
    String elasticVersion

    private AntBuilder ant
    private Project project

    StopElasticAction(Project project) {
        this.project = project
        this.ant = project.ant
    }

    void execute() {
        File toolsDir = toolsDir ?: new File("$project.rootDir/gradle/tools")
        ElasticActions elastic = new ElasticActions(project, toolsDir, elasticVersion ?: DEFAULT_ELASTIC_VERSION)

        println "${CYAN}* elastic:$NORMAL stopping ElasticSearch"

        try {
            if (elastic.version.startsWith("2") || elastic.version.startsWith("5")) {
                def pidFile = new File(elastic.home, 'elastic.pid')
                if (!pidFile.exists()) {
                    println "${RED}* elastic:$NORMAL ${pidFile} not found"
                    println "${RED}* elastic:$NORMAL could not stop ElasticSearch, please check manually!"
                    return
                }
                def elasticPid = pidFile.text
                println "${CYAN}* elastic:$NORMAL going to kill pid $elasticPid"

                "kill $elasticPid".execute()
            } else {
                newInstance().execute(Post("http://localhost:${httpPort ?: 9200}/_shutdown"))
            }

            println "${CYAN}* elastic:$NORMAL waiting for ElasticSearch to shutdown"
            ant.waitfor(maxwait: 2, maxwaitunit: "minute", timeoutproperty: "elasticTimeout") {
                not {
                    ant.http(url: "http://localhost:${httpPort ?: 9200}")
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
