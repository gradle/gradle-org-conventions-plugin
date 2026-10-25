package io.github.gradle.conventions.publishing;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.credentials.HttpHeaderCredentials;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.authentication.http.HttpHeaderAuthentication;

/**
 * Configures publishing to the Gradle internal repository for every project that applies
 * {@code maven-publish}, so that individual builds carry no repository or credential logic.
 *
 * <p>A single Maven repository named {@value #REPOSITORY_NAME} is always registered, which keeps
 * {@code publishAllPublicationsToGradleInternalRepository} discoverable and identically named
 * locally and on CI. Only its target changes:
 *
 * <ul>
 *     <li>When {@value #REPOSITORY_URL_ENV_NAME} is set (CI), it points at that repository and
 *     authenticates with an {@code Authorization} header.</li>
 *     <li>Otherwise (local development), it points at {@code build/}{@value #LOCAL_STAGING_DIR_NAME}
 *     and needs no credentials at all.</li>
 * </ul>
 *
 * <p>Credentials are never read by this plugin. The remote repository declares
 * {@link HttpHeaderCredentials} without any values, which makes Gradle resolve them from the
 * {@code gradleInternalAuthHeaderName} / {@code gradleInternalAuthHeaderValue} Gradle properties,
 * conventionally supplied by CI as {@code ORG_GRADLE_PROJECT_*} environment variables. Compared to
 * populating the credentials explicitly this
 * <ul>
 *     <li>keeps publishing tasks compatible with the configuration cache — explicit credentials
 *     make {@code PublishToMavenRepository} opt out of it, disabling the cache for the whole
 *     build;</li>
 *     <li>fails with a message naming the missing properties, and only when a task publishing to
 *     this repository is actually in the task graph;</li>
 *     <li>keeps the token out of the build's own configuration state.</li>
 * </ul>
 */
public abstract class PublishingConventionsPlugin implements Plugin<Settings> {

    /**
     * Environment variable holding the base URL of the Gradle internal repository. When absent,
     * publishing targets a local staging directory instead.
     */
    public static final String REPOSITORY_URL_ENV_NAME = "GRADLE_INTERNAL_REPO_URL";

    /**
     * Name of the registered repository. It determines both the generated task names and the
     * {@code gradleInternalAuthHeaderName} / {@code gradleInternalAuthHeaderValue} property names
     * Gradle looks up for credentials.
     */
    public static final String REPOSITORY_NAME = "gradleInternal";

    static final String SNAPSHOTS_REPOSITORY_PATH = "libs-snapshots-local";
    static final String RELEASES_REPOSITORY_PATH = "libs-releases-local";
    static final String LOCAL_STAGING_DIR_NAME = "staging-repo";
    static final String SNAPSHOT_VERSION_SUFFIX = "-SNAPSHOT";

    private static final String MAVEN_PUBLISH_PLUGIN_ID = "maven-publish";

    @Override
    public void apply(Settings settings) {
        // Configure projects from the settings plugin without reaching across the project
        // boundary, so the build stays compatible with Isolated Projects.
        settings.getGradle().getLifecycle().beforeProject(PublishingConventionsPlugin::configureProject);
    }

    private static void configureProject(Project project) {
        project.getPluginManager().withPlugin(MAVEN_PUBLISH_PLUGIN_ID, appliedPlugin -> {
            PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
            publishing.getRepositories().maven(repository -> configureRepository(project, repository));
        });
    }

    private static void configureRepository(Project project, MavenArtifactRepository repository) {
        repository.setName(REPOSITORY_NAME);

        String repositoryBaseUrl = trimTrailingSlashes(
            project.getProviders().environmentVariable(REPOSITORY_URL_ENV_NAME).getOrElse("")
        );

        if (repositoryBaseUrl.isEmpty()) {
            repository.setUrl(project.getLayout().getBuildDirectory().dir(LOCAL_STAGING_DIR_NAME));
            return;
        }

        repository.setUrl(remoteUrl(project, repositoryBaseUrl));
        // Deliberately no values: Gradle resolves them lazily from Gradle properties.
        repository.credentials(HttpHeaderCredentials.class);
        repository.getAuthentication().create("header", HttpHeaderAuthentication.class);
    }

    /**
     * The snapshot/release decision has to stay lazy: {@code beforeProject} runs before the
     * project's build script, so the version is still {@code unspecified} at that point.
     */
    private static Provider<String> remoteUrl(Project project, String repositoryBaseUrl) {
        return project.provider(() -> {
            String path = isSnapshot(project) ? SNAPSHOTS_REPOSITORY_PATH : RELEASES_REPOSITORY_PATH;
            return repositoryBaseUrl + "/" + path;
        });
    }

    private static boolean isSnapshot(Project project) {
        return String.valueOf(project.getVersion()).endsWith(SNAPSHOT_VERSION_SUFFIX);
    }

    private static String trimTrailingSlashes(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }
}
