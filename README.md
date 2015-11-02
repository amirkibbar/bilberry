# gradle-elastic-plugin
an ElasticSearch gradle plugin for integration tests with ElasticSearch

[ ![Build Status](https://travis-ci.org/amirkibbar/bilberry.svg?branch=master) ](https://travis-ci.org/amirkibbar/bilberry)
[ ![Download](https://api.bintray.com/packages/amirk/maven/gradle-elastic-plugin/images/download.svg) ](https://bintray.com/amirk/maven/gradle-elastic-plugin/_latestVersion)

# Using

Plugin setup with gradle >= 2.1:

```gradle

    plugins {
        id "ajk.gradle.elastic" version "0.0.5"
    }
```

Plugin setup with gradle < 2.1:

```gradle

    buildscript {
        repositories {
            jcenter()
            maven { url "http://dl.bintray.com/amirk/maven" }
        }
        dependencies {
            classpath("ajk.gradle.elastic:gradle-elastic-plugin:0.0.5")
        }
    }

    apply plugin: 'ajk.gradle.elastic'
```

# Starting and stopping ElasticSearch during the integration tests

```gradle

    task integrationTests(type: Test) {
        reports {
            html.destination "$buildDir/reports/integration-tests"
        }

        include "**/*IT.*"

        doFirst {
            startElastic {
				elasticVersion = "1.5.2"
                httpPort = 9200
				transportPort = 9300
				dataDir = file("$buildDir/elastic")
				logsDir = file("$buildDir/elastic/logs")
            }
        }
    
        doLast {
            stopElastic {
                httpPort = 9200
            }
        }
    }
    
    gradle.taskGraph.afterTask { Task task, TaskState taskState ->
        if (task.name == "integrationTests") {
            stopElastic {
                httpPort = 9200
            }
        }
    }

    test {
        include '**/*Test.*'
        exclude '**/*IT.*'
    }
```

The above example shows a task called integrationTests which runs all the tests in the project with the IT suffix. The
reports for these tests are placed in the buildDir/reports/integration-tests directory - just to separate them from
regular tests. But the important part here is in the doFirst and doLast. 

In the doFirst ElasticSearch is started. All the values in the example above are the default values, so if these values
work for you they can be ommitted:

```gradle

    doFirst {
        startElastic {}
    }
```

In the doLast ElasticSearch is stopped. Note that ElasticSearch is also stopped in the gradle.taskGraph.afterTask 
section - this is to catch any crashes during the integration tests and make sure that ElasticSearch is stopped in the 
build clean-up phase.

Lastly the regular test task is configured to exclude the tests with the IT suffix - we only wanted to run these in the
integration tests phase, not with the regular tests.

# More information

This plugin installs ElasticSearch locally at project.rootDir/gradle/tools/elastic. In addition, the [ElasticSearch 
head plugin](https://github.com/mobz/elasticsearch-head) is also installed. This plugin provides a nice web-based UI for
ElasticSearch, which I find useful. To access the UI start ElasticSearch with this plugin, the go to 
[http://localhost:9200/_plugin/head](http://localhost:9200/_plugin/head).

# References

- [ElasticSearch](https://www.elastic.co/products/elasticsearch)
