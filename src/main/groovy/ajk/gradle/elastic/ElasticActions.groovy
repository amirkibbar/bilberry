package ajk.gradle.elastic

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.Project

import static ajk.gradle.elastic.ElasticPlugin.CYAN
import static ajk.gradle.elastic.ElasticPlugin.NORMAL
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
    if (!new File("$home/bin/elasticsearch").exists()) return false

    boolean desiredVersion = isDesiredVersion()

    if (!desiredVersion) {
      // this is not the desired version, then we also need to delete the home directory
      ant.delete(dir: home)

      return false
    }

    return true
  }

  boolean isDesiredVersion() {
    println "${CYAN}* elastic:$NORMAL checking existing version"

    def versionFile = new File("$home/version.txt")
    if(!versionFile.exists()) return false

    def detectedVersion = versionFile.text

    println "${CYAN}* elastic:$NORMAL: detected version: $detectedVersion"

    return detectedVersion.contains(version)
  }

  void install() {
    if(isInstalled()) {
      println "${CYAN}* elastic:$NORMAL elastic search version $version detected at $home"
      return
    }

    println "${CYAN}* elastic:$NORMAL installing elastic version $version"

    String linuxUrl = "https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-${version}.tar.gz"
    String winUrl = "https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-${version}.zip"

    if (version.startsWith("5")) {
      linuxUrl = "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${version}.tar.gz"
      winUrl = "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${version}.zip"
    } else if (version.startsWith("2")) {
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
      ant.chmod(file: new File("$home/bin/plugin"), perm: "+x")
    }

    new File("$home/version.txt") << "$version"
  }
}
