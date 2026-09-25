package pl.olafcio.github

import org.gradle.api.Project
import pl.olafcio.github.plugins.Downloader

import javax.inject.Inject

class GitHubPlugins {
    private Project project

    @Inject
    GitHubPlugins(Project project) {
        this.project = project
    }

    Downloader github(String repository) {
        return new Downloader(repository, project)
    }
}
