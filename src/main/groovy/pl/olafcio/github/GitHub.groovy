package pl.olafcio.github

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

class GitHub implements Plugin<Project> {
    private static Project project

    @Override
    void apply(Project target) {
        project = target

        target.extensions.add("github_plugins", GitHubPlugins)

        target.extensions.add("github", (...obj) -> {
            var String              path
            var Map<String, String> properties

            if (obj.length == 1) {
                path = obj[0]
                properties = Map.of()
            } else if (obj.length == 2) {
                path = obj[1]
                properties = (Map<String, String>)obj[0]
            } else {
                throw new RuntimeException("'github' method overload not found: " + obj)
            }

            var parts = path.split("/", 2)

            var String name = parts[1]
            var String version = properties['version']

            return target.files(download(path, version, properties.getOrDefault('file', "${name}-${version}.jar")))
        })
    }

    static Path download(String path, String version, String filename, Path libs = null) {
        if (libs == null)
            libs = project.file(".gradle/gradle-github-plugin").toPath()

        Files.createDirectories(libs)

        def lib = libs.resolve(filename)
        def libF = lib.toFile()

        try {
            downloadTo("https://github.com/${path}/releases/download/${version}/${filename}", libF)
        } catch (ignored) {
            try {
                // Cannot use API due to restrictions. Requiring an API key would be insane.
                var items = URI.create("https://github.com/${path}/releases/expanded_assets/${version}")
                                        .toURL()
                                        .getText()
                                        .split(Pattern.quote('<div data-view-component="true" class="d-flex flex-justify-start flex-items-center col-12 col-lg-6">'))

                if (items.length != 4)
                    throw new RuntimeException("Cannot find JAR file for ${path} ${version} (filename = ${filename}) [tried release, expandedAssets has multiple files]")

                var link = items[1].split('<a href="')[1].split('"')[0]
                downloadTo("https://github.com" + link, libF = (lib = libs.resolve(link.split("/").last())).toFile())
            } catch (e2) {
                e2.printStackTrace()
                throw new RuntimeException("Cannot find JAR file for ${path} ${version} (filename = ${filename}) [tried release+expandedAssets]", e2)
            }
        }

        return lib
    }

    private static void downloadTo(String url, File file) {
        if (file.exists())
            return

        URI.create(url)
           .toURL()
           .openStream()
           .with {
                try (var f = new FileOutputStream(file)) {
                    transferTo(f)
                }

                close()
           }

        println "[GitHub] Downloaded '${url}'"
    }
}
