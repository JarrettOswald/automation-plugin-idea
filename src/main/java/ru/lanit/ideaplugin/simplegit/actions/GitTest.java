package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;

/*
public class GitTest extends AnAction {

    public GitTest() {
        super(IconLoader.getIcon("/upload-icon.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        String txt= Messages.showInputDialog(project, "What is your name?",
                "Input your name", Messages.getQuestionIcon());
        String componentName = "com.intellij.openapi.vcs.changes.VcsDirtyScopeManagerImpl";
        ProjectLevelVcsManagerImpl vcsManager = project.getComponent(ProjectLevelVcsManagerImpl.class);
        if (vcsManager != null) {
            Messages.showMessageDialog(project, "Hello, " + txt + "!\nhaveVcses: " + vcsManager.haveVcses(),
                    "Information", Messages.getInformationIcon());
            GitPushSupport pushSupport = ServiceManager.getService(e.getProject(), GitPushSupport.class);
            GitPushSource source = pushSupport.getSource(repository); // this simply creates the GitPushSource wrapper around the current branch or current revision in case of the detached HEAD
            GitPushTarget target = // create target either directly, or by using some methods from GitPushSupport. Just check them, most probably you'll find what's needed.
            Map<GitRepository, PushSpec<GitPushSource, GitPushTarget> pushSpecs = Collections.singletonMap(repository, new PushSpec(source, target));
            pushSupport.getPusher().push(specs, null, false);
        } else {
            Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.",
                    "Information", Messages.getInformationIcon());
        }
        System.out.println("Hello world!");
    }

    private boolean repositoryChanging = false;

    private void subscribeToRepoChangeEvents(@NotNull final Project project) {
        project.getMessageBus().connect().subscribe(GitRepository.GIT_REPO_CHANGE, new GitRepositoryChangeListener() {
            @Override
            public void repositoryChanged(@NotNull final GitRepository repository) {
                if (repositoryChanging) {
                    // We are already in the middle of a change, so ignore the event
                    // There is a case where we get into an infinite loop here if we don't ignore the message
//                    logger.info("Ignoring repository changed event since we are already in the middle of a change.");
                } else {
                    try {
                        repositoryChanging = true;
//                        logger.info("repository changed");
                        ProjectRepoEventManager.getInstance().triggerServerEvents(EventContextHelper.SENDER_REPO_CHANGED, project, repository);
                        TfsTelemetryHelper.sendRepoChangedEvent(GitVcs.NAME, VcsHelper.isVstsRepo(project));
                    } finally {
                        repositoryChanging = false;
                    }
                }
            }
        });
    }

    private void pushOnClever(@NotNull DeployDialog dialog, @NotNull Project project) {
            Application application = dialog.getSelectedItem();
            VirtualFile gitRoot = LocalFileSystem.getInstance().findFileByIoFile(new File(application.deployment.repository));
            assert gitRoot != null;
        GitRepositoryManager repositoryManager = ServiceManager.getService(project, GitRepositoryManager.class);
        GitRepository repository = repositoryManager.getRepositoryForRoot(gitRoot);
        if (repository == null) return;

        GitRemote remote = getRemote(repository.getRemotes(), application.deployment.url);
        if (remote == null) return;
        GitRemoteBranch branch = getBranch(repository, remote);

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
}
*/