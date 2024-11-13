plugins {
    id("org.ajoberstar.grgit")
}

val versionProvider = Wrapper(computeProjectVersion())
allprojects {
    group = "com.frog-development.micronaut"
    version = versionProvider
}

/**
 * temporary workaround waiting for project.version to be provider aware.
 * <ul>
 * <li><a href="https://github.com/gradle/gradle/issues/25971">workaround</a></li>
 * <li><a href="https://github.com/gradle/gradle/issues/13672">Project "coordinates" should use providers</a></li>
 * </ul>
 */
class Wrapper(private val version: Provider<String>) {
    override fun toString(): String {
        return version.get()
    }
}

fun computeProjectVersion(): Provider<String> {
    val branchName = grgit.branch.current().name

    println("Current branch: $branchName")

    val computedVersion = when (branchName) {
        "HEAD" -> handleHead()
        "main" -> handleMain()
        else -> handleBranch(branchName)
    }

    println("Computed version: $computedVersion")

    return provider { computedVersion }
}

fun handleHead(): String {
    val githubRefName = System.getenv("GITHUB_REF_NAME")
    if (githubRefName == null || githubRefName.isEmpty()) {
        throw GradleException("One does not simply build from HEAD. Checkout to matching local branch !!")
    }
    return githubRefName
}

fun handleMain(): String {
    return "main-SNAPSHOT"
}

fun handleBranch(branchName: String): String {
    val matchBranchResult = """^(?<type>\w+)/(?<details>.+)?$""".toRegex().find(branchName)
    val branchType = matchBranchResult!!.groups["type"]?.value!!
    val branchDetails = matchBranchResult.groups["details"]?.value!!

    if (branchType == "release" || branchType == "hotfix") {
        return "$branchDetails-SNAPSHOT"
    }

    return "$branchType-$branchDetails-SNAPSHOT"
}
