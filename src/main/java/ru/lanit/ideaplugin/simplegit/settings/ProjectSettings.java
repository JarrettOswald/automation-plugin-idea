package ru.lanit.ideaplugin.simplegit.settings;

import com.intellij.ide.util.PropertyName;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NonNls;
import ru.lanit.ideaplugin.simplegit.tags.model.EditableFavoriteTagList;

public class ProjectSettings {
    private static final Logger log = Logger.getInstance(ProjectSettings.class);

    @NonNls public static final String PLUGIN_ACTIVE = "simplegit.settings.pluginActive";
    @NonNls public static final String FAVORITE_TAGS_LIST = "simplegit.settings.favoriteTagsList";
    @NonNls public static final String FEATURE_PATH = "simplegit.settings.featurePath";
    @NonNls public static final String GIT_REPOSITORY_ROOT_PATH = "simplegit.settings.gitRepositoryRootPath";
    @NonNls public static final String REMOTE_GIT_REPOSITORY_URL = "simplegit.settings.remoteGitRepositoryURL";
    @NonNls public static final String REMOTE_MAIN_BRANCH = "simplegit.settings.remoteMainBranch";

    @NonNls
    @PropertyName(value = PLUGIN_ACTIVE, defaultValue = "false")
    private boolean pluginActive;

    @PropertyName(value = FAVORITE_TAGS_LIST)
    private String favoriteTags;

    @PropertyName(value = FEATURE_PATH)
    private String featurePath;

    @PropertyName(value = GIT_REPOSITORY_ROOT_PATH)
    private String gitRepositoryRootPath;

    @PropertyName(value = REMOTE_GIT_REPOSITORY_URL)
    private String remoteGitRepositoryURL;

    @PropertyName(value = REMOTE_MAIN_BRANCH)
    private String remoteMainBranch;

    public boolean isPluginActive() {
        return pluginActive;
    }

    public void setPluginActive(boolean pluginActive) {
        this.pluginActive = pluginActive;
    }

    public EditableFavoriteTagList getFavoriteTags() {
        if (favoriteTags == null) {
            return new EditableFavoriteTagList();
        }
        return new EditableFavoriteTagList(favoriteTags.split(";"));
    }

    public void setFavoriteTags(EditableFavoriteTagList favoriteTags) {
        this.favoriteTags = favoriteTags == null ? "" : favoriteTags.toString();
    }

    public String getFeaturePath() {
        return featurePath == null ? "" : featurePath;
    }

    public void setFeaturePath(String featurePath) {
        this.featurePath = featurePath == null ? "" : featurePath;
    }

    public String getGitRepositoryRootPath() {
        return gitRepositoryRootPath == null ? "" : gitRepositoryRootPath;
    }

    public void setGitRepositoryRootPath(String gitRepositoryRootPath) {
        this.gitRepositoryRootPath = gitRepositoryRootPath == null ? "" : gitRepositoryRootPath;
    }

    public String getRemoteGitRepositoryURL() {
        return remoteGitRepositoryURL == null ? "" : remoteGitRepositoryURL;
    }

    public void setRemoteGitRepositoryURL(String remoteGitRepositoryURL) {
        this.remoteGitRepositoryURL = remoteGitRepositoryURL == null ? "" : remoteGitRepositoryURL;
    }

    public void cleanup() {
        this.favoriteTags = favoriteTags == null ? "" : favoriteTags;
        this.featurePath = featurePath == null ? "" : featurePath;
        this.gitRepositoryRootPath = gitRepositoryRootPath == null ? "" : gitRepositoryRootPath;
        this.remoteGitRepositoryURL = remoteGitRepositoryURL == null ? "" : remoteGitRepositoryURL;
    }

    public String getRemoteMainBranch() {
        return remoteMainBranch;
    }

    public void setRemoteMainBranch(String remoteMainBranch) {
        this.remoteMainBranch = remoteMainBranch;
    }
}
