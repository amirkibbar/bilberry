package ajk.gradle.elastic

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.Project

import static org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import static org.apache.tools.ant.taskdefs.condition.Os.isFamily

class ElasticActions {
    String version
    File toolsDir
    Project project
    AntBuilder ant
    File home

    ElasticActions(Project project, File toolsDir, String version) {
        this.project = project
        this.toolsDir = toolsDir
        this.version = version
        this.ant = project.ant
        home = new File("$toolsDir/elastic")
    }

    boolean isInstalled() {
        return new File("$home/bin/elasticsearch").exists()
    }

    void install(List<String> withPlugins) {
        println "* elastic: installing elastic version $version"
        String linuxUrl = "https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-${version}.tar.gz"
        String winUrl = "https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-${version}.zip"

        if (version.startsWith("2")) {
            linuxUrl = "https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/tar/elasticsearch/${version}/elasticsearch-${version}.tar.gz"
            winUrl = "https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/${version}/elasticsearch-${version}.zip"
        }

        String elasticPackage = isFamily(FAMILY_WINDOWS) ? winUrl : linuxUrl
        File elasticFile = new File("$toolsDir/elastic-${version}.zip")

        DownloadAction elasticDownload = new DownloadAction(project)
        elasticDownload.dest(elasticFile)
        elasticDownload.src(elasticPackage)
        elasticDownload.onlyIfNewer(true)
        elasticDownload.execute()

        ant.delete(dir: home, quiet: true)
        home.mkdirs()

        if (isFamily(FAMILY_WINDOWS)) {
            ant.unzip(src: elasticFile, dest: "$home") {
                cutdirsmapper(dirs: 1)
            }
        } else {
            ant.untar(src: elasticFile, dest: "$home", compression: "gzip") {
                cutdirsmapper(dirs: 1)
            }
            ant.chmod(file: new File("$home/bin/elasticsearch"), perm: "+x")
        }

        if (withPlugins.contains("head plugin")) {
            println "* elastic: installing the head plugin"
            String plugin = "$home/bin/plugin"
            if (isFamily(FAMILY_WINDOWS)) {
                plugin += ".bat"
            }

            [
                    new File(plugin),
                    "--install",
                    "mobz/elasticsearch-head"
            ].execute([
                    "JAVA_HOME=${System.properties['java.home']}",
                    "JAVA_OPTS=${System.getenv("JAVA_OPTS")}",
                    "ES_HOME=$home"

            ], home)
        }
    }
}
