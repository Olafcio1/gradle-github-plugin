package pl.olafcio.github.plugins

import groovy.transform.TupleConstructor
import org.gradle.api.Plugin
import org.gradle.api.Project
import pl.olafcio.github.GitHub

import java.util.zip.ZipFile

@TupleConstructor
class Downloader {
    String path
    Project project

    private String file

    Downloader file(String filename) {
        this.file = filename
        return this
    }

    void version(String version) {
        var name = path.substring(path.indexOf("/") + 1)
        var path = GitHub.download(
                path, version,
                (String) Objects.requireNonNullElse(this.file, "${name}-${version}.jar")
        )

        var plugins = new ArrayList<PluginRecord>()

        try (var zip = new ZipFile(path.toFile())) {
            var entries = zip.entries()

            while (entries.hasMoreElements()) {
                var entry = entries.nextElement()
                if (entry.name.startsWith("META-INF/gradle-plugins/") && entry.name.endsWith(".properties")) {
                    var pluginID = entry.name.substring(24, entry.name.length() - 11)
                    var pluginClassName

                    try (var stream = zip.getInputStream(entry)) {
                        pluginClassName = new String(stream.readAllBytes()).split("implementation-class=")[1].split("\n")[0].trim()
                    }

                    plugins.add(new PluginRecord(pluginID, pluginClassName))
                }
            }
        }

        try (var jar = new URLClassLoader(new URL[]{ path.toUri().toURL() }, this.class.getClassLoader())) {
            for (var plugin : plugins) {
                try {
                    project.pluginManager.apply(jar.loadClass(plugin.className()) as Class<? extends Plugin>)
                }catch(e){e.printStackTrace();throw e}
            }
        }
    }
}
