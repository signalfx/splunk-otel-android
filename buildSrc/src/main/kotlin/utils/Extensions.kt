import groovy.util.Node
import groovy.util.NodeList
import org.codehaus.groovy.runtime.StringGroovyMethods.center
import org.gradle.api.Project

fun Project.optProperty(name: String): Any? {
    return if (project.hasProperty(name)) {
        project.property(name)
    } else {
        null
    }
}

fun Project.propOrDefault(name: String, default: Any): Any? {
    return if (project.hasProperty(name)) {
        project.property(name)
    } else {
        default
    }
}

fun String.toBoxString(): String {
    return "║" + center(this, 98) + "║"
}

fun Any?.wrapAsBuildConfigField(): String = this?.run { "\"" + this.toString() + "\"" } ?: "\"\""

fun Node.addSplunkInfo() {
    if ((get("name") as NodeList).size == 0) {
        appendNode("name", "Splunk RUM Agent for Android")
    }
    appendNode("description", "Splunk's Real User Monitoring Agent for your Android application.")
    appendNode("url", "https://www.splunk.com")
    appendNode("licenses")
        .appendNode("license")
        .appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0.txt")

    val developerNode = appendNode("developers")
        .appendNode("developer")
    developerNode.appendNode("id", "splunk")
    developerNode.appendNode("name", "Splunk Instrumentation Authors")
    developerNode.appendNode("email", "support+java@signalfx.com")
    developerNode.appendNode("organization", "Splunk Inc.")
    developerNode.appendNode("organizationUrl", "https://splunk.com/")

    val scmNode = appendNode("scm")
    scmNode.appendNode("connection", "https://github.com/signalfx/splunk-otel-android.git")
    scmNode.appendNode("url", "https://github.com/signalfx/splunk-otel-android")
}
