package ru.lanit.ideaplugin.simplegit;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshSession;
import git4idea.GitBranch;
import git4idea.GitRemoteBranch;
import git4idea.GitStandardRemoteBranch;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.dialogs.newfeature.NewFeatureDialog;
import ru.lanit.ideaplugin.simplegit.dialogs.pluginsettings.PluginSettingsDialog;
import ru.lanit.ideaplugin.simplegit.features.FeatureList;
import ru.lanit.ideaplugin.simplegit.settings.PluginSettings;
import ru.lanit.ideaplugin.simplegit.settings.PluginSettingsProvider;
import ru.lanit.ideaplugin.simplegit.settings.SettingsChangeListener;

import java.util.Collection;
import java.util.List;

public class SimpleGitProjectComponent implements ProjectComponent, SettingsChangeListener {
    private final Project project;
    private FeatureList featureList;
    private PluginSettingsProvider settings;
    private RefreshSession refreshSession;
    private GitRepositoryManager repositoryManager;

    public SimpleGitProjectComponent(Project project) {
        this.project = project;
//        this.refreshSession = RefreshQueue.getInstance().createSession(true, true, null);
        settings = new PluginSettingsProvider(project, this);
        settings.restoreAllSettings();
        repositoryManager = ServiceManager.getService(project, GitRepositoryManager.class);
    }

    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "SimpleGit";
    }

    public void projectOpened() {
        // called when project is opened
        this.featureList = ServiceManager.getService(project, FeatureList.class);
    }

    public Project getProject() {
        return project;
    }

    public void createNewScenario() {
        System.out.println("Create new scenario in project " + project.getBasePath());
        NewFeatureDialog newFeatureDialog = new NewFeatureDialog(project);
        newFeatureDialog.show();

    }

    public void gitSynchronize() {
        System.out.println("Git synchronize project " + project.getBasePath());
        /*String txt= Messages.showInputDialog(project, "What is your name?",
                "Input your name", Messages.getQuestionIcon());*/
//        Messages.showMessageDialog(project, "Push is not implemented",
//                "Information", Messages.getInformationIcon());
        featureList.updateFeatures();
        pushGit();
        /*
        GitPushSupport pushSupport = ServiceManager.getService(project, GitPushSupport.class);
        GitPushSource source = pushSupport.getSource(repository); // this simply creates the GitPushSource wrapper around the current branch or current revision in case of the detached HEAD
        GitPushTarget target = // create target either directly, or by using some methods from GitPushSupport. Just check them, most probably you'll find what's needed.
        Map<GitRepository, PushSpec<GitPushSource, GitPushTarget> pushSpecs = Collections.singletonMap(repository, new PushSpec(source, target));
        pushSupport.getPusher().push(specs, null, false);*/

    }
    private boolean repositoryChanging = false;

    private void subscribeToRepoChangeEvents(@NotNull final Project project) {
        project.getMessageBus().connect().subscribe(GitRepository.GIT_REPO_CHANGE, (@NotNull final GitRepository repository) -> {
            if (repositoryChanging) {
                // We are already in the middle of a change, so ignore the event
                // There is a case where we get into an infinite loop here if we don't ignore the message
                System.out.println("Ignoring repository changed event since we are already in the middle of a change.");
            } else {
                try {
                    repositoryChanging = true;
//                        logger.info("repository changed");
                    System.out.println("Repo changed " + repository.toString());
                } finally {
                    repositoryChanging = false;
                }
            }
        });
    }

    public boolean gitRepositoryExists() {
        return getGitRepository() != null;
    }

    public List<GitRepository> getGitRepositories() {
        VirtualFile gitRoot = project.getBaseDir();
        return repositoryManager.getRepositories();
    }

    public Collection<GitRemote> getRemoteGitRepositories(GitRepository repository){
        return repository.getRemotes();
    }

    private GitRepository getGitRepository() {
        VirtualFile gitRoot = project.getBaseDir();
        repositoryManager.getRepositories();
        return repositoryManager.getRepositoryForRoot(gitRoot);
    }

    private void pushGit() {
        GitRepository repository = getGitRepository();
        if (repository == null) {
            /*repository = GitRepositoryImpl.getInstance(gitRoot, project, true);
            repositoryManager.addExternalRepository(gitRoot, repository);
            repository*/
            return;
        }

        GitRemote remote = getRemote(repository.getRemotes(), "");
        if (remote == null) return;
        GitRemoteBranch branch = getBranch(repository, remote);
/*
        ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);
        AbstractVcs abstractVcs = projectLevelVcsManager.getVcsFor(gitRoot);
        assert abstractVcs != null;
        GitPushSupport pushSupport = (GitPushSupport) DvcsUtil.getPushSupport(abstractVcs);
        assert pushSupport != null;
        GitPushSource source = pushSupport.getSource(repository);
        GitPushTarget target = new GitPushTarget(branch, false);

        PushSpec<GitPushSource, GitPushTarget> pushSourceGitPushTargetPushSpec = new PushSpec<>(source, target);
        Map<GitRepository, PushSpec<GitPushSource, GitPushTarget>> pushSpecs =
                Collections.singletonMap(repository, pushSourceGitPushTargetPushSpec);

        pushSupport.getPusher().push(pushSpecs, null, false);
        */
    }

    private GitRemote getRemote(@NotNull Collection<GitRemote> gitRemoteCollections, String remoteUrl) {
        for (GitRemote gitRemote : gitRemoteCollections) {
            int remoteIndex = gitRemote.getUrls().indexOf(remoteUrl);
            if (remoteIndex != -1) {
                return gitRemote;
            }
        }
        return null;
    }

    @NotNull
    private GitRemoteBranch getBranch(@NotNull GitRepository repository, @NotNull GitRemote remote) {
        for (GitRemoteBranch remoteBranch : repository.getBranches().getRemoteBranches()) {
            if (remoteBranch.getRemote().equals(remote) && remoteBranch.getName().equals("master")) return remoteBranch;
        }
        return new GitStandardRemoteBranch(remote, "master", GitBranch.DUMMY_HASH);
    }

    public void openOptionsWindow() {
        System.out.println("Open settings dialog for project " + project.getBasePath());
        PluginSettingsDialog pluginSettingsDialog = new PluginSettingsDialog(project);
        settings.setSettingsToDialog(pluginSettingsDialog);
        pluginSettingsDialog.show();
        if (pluginSettingsDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            settings.setSettingsFromDialog(pluginSettingsDialog);
            Messages.showMessageDialog(project, "Selected feature path: " + pluginSettingsDialog.getFeaturePath(),
                    "Information", Messages.getInformationIcon());
        }
    }

    public boolean isPluginActive() {
        return settings.isPluginActive();
    }

    public String getFeaturePath() {
        System.out.println("getFeaturePath");
        return getFeatureDir().getPath();
    }

    public VirtualFile getFeatureDir() {
        System.out.println("getFeatureDir");
        String basePath = project.getBasePath();
        VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
        return baseDir.findFileByRelativePath(settings.getFeaturePath());
    }

    @Override
    public void onSettingsChange(PluginSettings newOptions, PluginSettings oldOptions) {
        if (oldOptions != null && newOptions.isPluginActive() && !newOptions.getFeaturePath().equals(oldOptions.getFeaturePath())) {
            featureList.updateFeatures();
        }
    }

    public void projectClosed() {
        // called when project is being closed
    }
}