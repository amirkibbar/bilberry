package ajk.gradle.elastic

import org.gradle.api.Plugin
import org.gradle.api.Project

class ElasticPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('startElastic', StartElasticExtension, project)
        project.extensions.create('stopElastic', StopElasticExtension, project)
    }
}
