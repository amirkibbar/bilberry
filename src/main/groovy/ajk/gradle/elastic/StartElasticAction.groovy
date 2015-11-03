package ajk.gradle.elastic

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import static ajk.gradle.elastic.ElasticPlugin.*
import static org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import static org.apache.tools.ant.taskdefs.condition.Os.isFamily

class StartElasticAction {

    @Input
    @Optional
    String elasticVersion

    @Input
    @Optional
    int httpPort

    @Input
    @Optional
    int transportPort

    @Input
    @Optional
    File toolsDir

    @Input
    @Optional
    File dataDir

    @Input
    @Optional
    File logsDir

    @Input
    @Optional
    List<String> withPlugins = ["head plugin"]

    private Project project

    private AntBuilder ant

    StartElasticAction(Project project) {
        this.project = project
        this.ant = project.ant
    }

    void execute() {
        File toolsDir = toolsDir ?: new File("$project.rootDir/gradle/tools")
        ElasticActions elastic = new ElasticActions(project, toolsDir, elasticVersion ?: DEFAULT_ELASTIC_VERSION)

        if (!elastic.installed) {
            elastic.install(withPlugins)
        }

        httpPort = httpPort ?: 9200
        transportPort = transportPort ?: 9300
        dataDir = dataDir ?: new File("$project.buildDir/elastic")
        logsDir = logsDir ?: new File("$dataDir/logs")
        println "${CYAN}* elastic:$NORMAL starting ElasticSearch at $elastic.home using http port $httpPort and tcp transport port $transportPort"
        println "${CYAN}* elastic:$NORMAL ElasticSearch data directory: $dataDir"
        println "${CYAN}* elastic:$NORMAL ElasticSearch logs directory: $logsDir"

        ant.delete(failonerror: true, dir: dataDir)
        ant.delete(failonerror: true, dir: logsDir)
        logsDir.mkdirs()
        dataDir.mkdirs()

        File esScript = new File("${elastic.home}/bin/elasticsearch${isFamily(FAMILY_WINDOWS) ? '.bat' : ''}")

        [
                esScript.absolutePath,
                "-p${new File(elastic.home, 'elastic.pid')}",
                "-Des.http.port=$httpPort",
                "-Des.transport.tcp.port=$transportPort",
                "-Des.path.data=$dataDir",
                "-Des.path.logs=$logsDir",
                "-Des.discovery.zen.ping.multicast.enabled=false"

        ].execute([
                "JAVA_HOME=${System.properties['java.home']}",
                "ES_HOME=$elastic.home",
                "ES_MAX_MEM=512m",
                "ES_MIN_MEM=128m"
        ], elastic.home)

        println "${CYAN}* elastic:$NORMAL waiting for ElasticSearch to start"
        ant.waitfor(maxwait: 2, maxwaitunit: "minute", timeoutproperty: "elasticTimeout") {
            and {
                socket(server: "localhost", port: transportPort)
                ant.http(url: "http://localhost:$httpPort")
            }
        }

        if (ant.properties['elasticTimeout'] != null) {
            println "${RED}* elastic:$NORMAL could not start ElasticSearch"
            throw new RuntimeException("failed to start ElasticSearch")
        } else {
            println "${CYAN}* elastic:$NORMAL ElasticSearch is now up"
        }
    }
}
