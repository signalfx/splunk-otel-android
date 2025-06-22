import groovy.util.Node
import groovy.util.NodeList
import org.codehaus.groovy.runtime.StringGroovyMethods.center
import org.gradle.api.Project

fun Project.getVersionPostfix(): String {
    return if (rootProject.hasProperty("maven.deploy.artifactory.snapshot")) {
        val postfix = rootProject.properties["maven.deploy.artifactory.snapshot"].toString()
        "-$postfix-TEST"
    } else {
        ""
    }
}

fun String.toBoxString(): String {
    return "║" + center(this, 98) + "║"
}

fun Node.addCiscoInfo() {
    if ((get("name") as NodeList).size == 0) {
        appendNode("name", "Cisco RUM Agent for Android")
    }
    appendNode("description", "Cisco's Real User Monitoring Agent for your Android application.")
    appendNode("url", "http://cisco.com")
    appendNode("licenses")
        .appendNode("license")
        .appendNode("url", "https://www.cisco.com/c/dam/en_us/about/doing_business/legal/eula/cisco_end_user_license_agreement-eng.pdf")

    val developerNode = appendNode("developers")
        .appendNode("developer")
    developerNode.appendNode("id", "ad")
    developerNode.appendNode("name", "AppDynamics Inc.")
    developerNode.appendNode("email", "info@appdynamics.com")
    developerNode.appendNode("organization", "AppDynamics Inc.")
    developerNode.appendNode("organizationUrl", "https://www.appdynamics.com/")

    val scmNode = appendNode("scm")
    scmNode.appendNode("connection", "scm:git:git@github.com:Appdynamics/android_agent.git")
    scmNode.appendNode("url", "git:git@github.com:Appdynamics/android_agent.git")
}
