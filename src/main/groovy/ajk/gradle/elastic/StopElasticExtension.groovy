package ajk.gradle.elastic

import org.gradle.api.Project
import org.gradle.util.Configurable

import static org.gradle.util.ConfigureUtil.configure

class StopElasticExtension implements Configurable<StopElasticExtension> {
    private Project project

    StopElasticExtension(Project project) {
        this.project = project
    }

    @Override
    StopElasticExtension configure(Closure closure) {
        configure(closure, new StopElasticAction(project)).execute()

        return this
    }
}
