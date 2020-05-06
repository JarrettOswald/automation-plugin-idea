package ru.lanit.ideaplugin.simplegit.git;

import com.intellij.mock.Mock;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.actions.DescindingFilesFilter;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.actions.VcsContextWrapper;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vcs.update.*;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.MultiMap;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitRemoteBranch;
import git4idea.GitStandardRemoteBranch;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.branch.GitBranchUtil;
import git4idea.i18n.GitBundle;
import git4idea.push.GitPushSupport;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.update.GitFetchResult;
import git4idea.update.GitFetcher;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;
import ru.lanit.ideaplugin.simplegit.settings.SettingsProvider;

import javax.swing.*;
import java.util.*;

import static com.intellij.openapi.vcs.VcsNotifier.NOTIFICATION_GROUP_ID;

public class GitManager {
    private static final Logger log = Logger.getInstance(GitManager.class);

    private final SimpleGitProjectComponent plugin;
    private boolean repositoryChanging = false;
    private GitRepositoryManager repositoryManager;
    private ActionInfo myActionInfo;
    private ScopeInfo myScopeInfo;

    public GitManager(SimpleGitProjectComponent plugin) {
        this.plugin = plugin;
        myScopeInfo = ScopeInfo.PROJECT;
        myActionInfo = ActionInfo.UPDATE;

        repositoryManager = ServiceManager.getService(plugin.getProject(), GitRepositoryManager.class);
        subscribeToRepoChangeEvents();
    }

    public void suggestRepository(SettingsProvider settings) {
        settings.setRemoteGitRepositoryURL("");
    }

    public boolean gitRepositoryExists() {
        return getGitRepository() != null;
    }

    public List<GitRepository> getGitRepositories() {
        VirtualFile gitRoot = plugin.getProject().getBaseDir();
        return repositoryManager.getRepositories();
    }

    public Collection<GitRemote> getRemoteGitRepositories(GitRepository repository) {
        return repository.getRemotes();
    }

    public void synchronizeGit(AnActionEvent event) {
        updateProject(event);
    }

    private GitRepository getGitRepository() {
        VirtualFile gitRoot = plugin.getProject().getBaseDir();
        repositoryManager.getRepositories();
        return repositoryManager.getRepositoryForRoot(gitRoot);
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

    private void fetchGit(ProgressIndicator indicator) {
        Project project = plugin.getProject();
        GitRepository repository = getGitRepository();

        GitFetcher fetcher = new GitFetcher(project, indicator, true);
        GitFetchResult result = fetcher.fetch(repository);

        if (!result.isSuccess()) {
            SwingUtilities.invokeLater(
                    () -> {
                        Notification notification =
                                new Notification(
                                        NOTIFICATION_GROUP_ID.getDisplayId(),
                                        "Fetch failed",
                                        "Fail to fetch " + repository.toString(),
                                        NotificationType.ERROR);
                        notification.notify(project);
                    });
        }
        repository.update();
    }

    @NonNls
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

    private void updateProject(AnActionEvent e) {
        try {
            VcsContext context = VcsContextWrapper.createCachedInstanceOn(e);
            Project project = plugin.getProject();
            final FilePath[] filePaths = myScopeInfo.getRoots(context, myActionInfo);
            final FilePath[] roots = DescindingFilesFilter.filterDescindingFiles(filterRoots(filePaths, context), project);
            if (roots.length == 0) {
                System.out.println("No roots found.");
                return;
            }

            final Map<AbstractVcs, Collection<FilePath>> vcsToVirtualFiles = createVcsToFilesMap(roots, project);

            for (AbstractVcs vcs : vcsToVirtualFiles.keySet()) {
                final UpdateEnvironment updateEnvironment = myActionInfo.getEnvironment(vcs);
                if ((updateEnvironment != null) && (!updateEnvironment.validateOptions(vcsToVirtualFiles.get(vcs)))) {
                    // messages already shown
                    System.out.println("Options not valid for files: " + vcsToVirtualFiles);
                    return;
                }
            }

            if (ApplicationManager.getApplication().isDispatchThread()) {
                ApplicationManager.getApplication().saveAll();
            }
            Task.Backgroundable task = new Updater(plugin, roots, vcsToVirtualFiles);
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                task.run(new EmptyProgressIndicator());
            } else {
                ProgressManager.getInstance().run(task);
            }
        } catch (ProcessCanceledException ignored) {
        }
    }

    private Map<AbstractVcs, Collection<FilePath>> createVcsToFilesMap(@NotNull FilePath[] roots, @NotNull Project project) {
        MultiMap<AbstractVcs, FilePath> resultPrep = MultiMap.createSet();
        for (FilePath file : roots) {
            AbstractVcs vcs = VcsUtil.getVcsFor(project, file);
            if (vcs != null) {
                UpdateEnvironment updateEnvironment = myActionInfo.getEnvironment(vcs);
                if (updateEnvironment != null) {
                    resultPrep.putValue(vcs, file);
                }
            }
        }

        final Map<AbstractVcs, Collection<FilePath>> result = new THashMap<>();
        for (Map.Entry<AbstractVcs, Collection<FilePath>> entry : resultPrep.entrySet()) {
            AbstractVcs<?> vcs = entry.getKey();
            result.put(vcs, vcs.filterUniqueRoots(new ArrayList<>(entry.getValue()), FilePath::getVirtualFile));
        }
        return result;
    }

    private FilePath[] filterRoots(FilePath[] roots, VcsContext vcsContext) {
        final ArrayList<FilePath> result = new ArrayList<>();
        final Project project = vcsContext.getProject();
        assert project != null;
        for (FilePath file : roots) {
            AbstractVcs vcs = VcsUtil.getVcsFor(project, file);
            if (vcs != null) {
                if (!myScopeInfo.filterExistsInVcs() || AbstractVcs.fileInVcsByFileStatus(project, file)) {
                    UpdateEnvironment updateEnvironment = myActionInfo.getEnvironment(vcs);
                    if (updateEnvironment != null) {
                        result.add(file);
                    }
                } else {
                    final VirtualFile virtualFile = file.getVirtualFile();
                    if (virtualFile != null && virtualFile.isDirectory()) {
                        final VirtualFile[] vcsRoots = ProjectLevelVcsManager.getInstance(vcsContext.getProject()).getAllVersionedRoots();
                        for (VirtualFile vcsRoot : vcsRoots) {
                            if (VfsUtilCore.isAncestor(virtualFile, vcsRoot, false)) {
                                result.add(file);
                            }
                        }
                    }
                }
            }
        }
        return result.toArray(new FilePath[result.size()]);
    }

    static boolean someSessionWasCanceled(List<UpdateSession> updateSessions) {
        for (UpdateSession updateSession : updateSessions) {
            if (updateSession.isCanceled()) {
                return true;
            }
        }
        return false;
    }

    private static String getAllFilesAreUpToDateMessage(FilePath[] roots) {
        if (roots.length == 1 && !roots[0].isDirectory()) {
            return VcsBundle.message("message.text.file.is.up.to.date");
        }
        else {
            return VcsBundle.message("message.text.all.files.are.up.to.date");
        }
    }

}
