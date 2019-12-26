package ru.lanit.ideaplugin.simplegit;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitRemoteBranch;
import git4idea.GitStandardRemoteBranch;
import git4idea.push.GitPushSupport;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.settings.PluginSettingsProvider;

import java.util.Collection;
import java.util.List;

public class GitManager {
    private final SimpleGitProjectComponent plugin;
    private boolean repositoryChanging = false;
    private GitRepositoryManager repositoryManager;

    GitManager(SimpleGitProjectComponent plugin) {
        this.plugin = plugin;

        repositoryManager = ServiceManager.getService(plugin.getProject(), GitRepositoryManager.class);
        subscribeToRepoChangeEvents();
    }

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

    public void synchronizeGit() {
        ProgressManager.getInstance().run(new Task.Backgroundable(plugin.getProject(), "Push to Git", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                pushGit();
            }
        });
    }

    public boolean gitRepositoryExists() {
        return getGitRepository() != null;
    }

    public List<GitRepository> getGitRepositories() {
        VirtualFile gitRoot = plugin.getProject().getBaseDir();
        return repositoryManager.getRepositories();
    }

    public Collection<GitRemote> getRemoteGitRepositories(GitRepository repository){
        return repository.getRemotes();
    }

    private GitRepository getGitRepository() {
        VirtualFile gitRoot = plugin.getProject().getBaseDir();
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
        return new GitStandardRemoteBranch(remote, "master");
    }

    public boolean setGit() {
        ProjectLevelVcsManagerImpl vcsManager = plugin.getProject().getComponent(ProjectLevelVcsManagerImpl.class);
        if (vcsManager != null) {
            GitRepositoryManager gitRepoManager;
            GitPushSupport pushSupport = ServiceManager.getService(plugin.getProject(), GitPushSupport.class);/*
            GitPushSource source = pushSupport.getSource(repository); // this simply creates the GitPushSource wrapper around the current branch or current revision in case of the detached HEAD
            GitPushTarget target = // create target either directly, or by using some methods from GitPushSupport. Just check them, most probably you'll find what's needed.
            Map<GitRepository, PushSpec<PushSource, GitPushTarget> pushSpecs = Collections.singletonMap(repository, new PushSpec(source, target));
            pushSupport.getPusher().push(specs, null, false);*/
            return true;
        }
        return false;
    }

    private void subscribeToRepoChangeEvents() {
        plugin.getProject().getMessageBus().connect().subscribe(GitRepository.GIT_REPO_CHANGE, (@NotNull final GitRepository repository) -> {
            if (repositoryChanging) {
                // We are already in the middle of a change, so ignore the event
                // There is a case where we get into an infinite loop here if we don't ignore the message
                System.out.println("Ignoring repository changed event since we are already in the middle of a change.");
            } else {
                try {
                    repositoryChanging = true;
                    System.out.println("repository changed");
                } finally {
                    repositoryChanging = false;
                }
            }
        });
    }

    public void suggestRepository(PluginSettingsProvider settings) {
        settings.setRemoteGitRepositoryURL("");
    }
}
