package ajk.gradle.elastic

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.Project

import static ajk.gradle.elastic.ElasticPlugin.CYAN
import static ajk.gradle.elastic.ElasticPlugin.NORMAL
import static org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import static org.apache.tools.ant.taskdefs.condition.Os.isFamily

class ElasticActions {
  String url
  String version
  File toolsDir
  Project project
  AntBuilder ant
  File home

  ElasticActions(Project project, File toolsDir, String version, String url) {
    this.project = project
    this.toolsDir = toolsDir
    this.version = version
    this.url = url
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

    return detectedVersion.equals(version)
  }

  void install() {
    if (isInstalled()) {
      println "${CYAN}* elastic:$NORMAL elastic search version $version detected at $home"
      return
    }

    println "${CYAN}* elastic:$NORMAL installing elastic version $version"

    if (this.url) {
      installFromUrl()
    } else {
      installFromElastic()
    }

    doExtract()
  }

  void installFromUrl() {
    doDownload(this.url)
  }

  void installFromElastic() {
    def majorVersion = Integer.valueOf( version.split( "\\." )[0] )

    String url

    // NOTE: there is no difference between the tar and zip distros
    switch (majorVersion) {
      case  0:
      case  1:
        url = "https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-${version}.zip"
        break

      case  2:
        url = "https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/${version}/elasticsearch-${version}.zip"
        break

      // there are no versions 3 and 4

      default: // catches version 5 and up
        url = "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${version}.zip"
        break
    }

    doDownload(url)
  }

  void doDownload(binaryUrl) {
    File elasticFile = new File("$toolsDir/elastic-${version}.zip")

    DownloadAction elasticDownload = new DownloadAction(project)
    elasticDownload.dest(elasticFile)
    elasticDownload.src(binaryUrl)
    elasticDownload.onlyIfNewer(true)
    elasticDownload.execute()
  }

  void doExtract() {
    ant.delete(dir: home, quiet: true)
    home.mkdirs()

    ant.unzip(src: "$toolsDir/elastic-${version}.zip", dest: "$home") {
      cutdirsmapper(dirs: 1)
    }

    ant.chmod(file: new File("$home/bin/elasticsearch"), perm: "+x")
    ant.chmod(file: new File("$home/bin/plugin"), perm: "+x")

    new File("$home/version.txt").text = version
  }
}
