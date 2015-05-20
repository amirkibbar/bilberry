package ajk.gradle.elastic

import org.gradle.api.Project
import org.gradle.util.Configurable

import static org.gradle.util.ConfigureUtil.configure

class StartElasticExtension implements Configurable<StartElasticExtension> {
    private Project project

    StartElasticExtension(Project project) {
        this.project = project
    }

    @Override
    StartElasticExtension configure(Closure closure) {
        configure(closure, new StartElasticAction(project)).execute()

        return this
    }
}
