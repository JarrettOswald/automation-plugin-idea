package ru.lanit.ideaplugin.simplegit.git;

import com.google.common.collect.ImmutableList;
import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.push.PushSpec;
import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.actions.DescindingFilesFilter;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.actions.VcsContextWrapper;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vcs.update.*;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.*;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.Git;
import git4idea.push.GitPushSource;
import git4idea.push.GitPushSupport;
import git4idea.push.GitPushTarget;
import git4idea.repo.GitBranchTrackInfo;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.update.GitFetchResult;
import git4idea.update.GitFetcher;
import gnu.trove.THashMap;
import org.jetbrains.annotations.CalledInAwt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;
import ru.lanit.ideaplugin.simplegit.actions.GitSynchronizeAction;
import ru.lanit.ideaplugin.simplegit.features.FeatureList;
import ru.lanit.ideaplugin.simplegit.settings.SettingsProvider;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static com.intellij.openapi.vcs.VcsNotifier.NOTIFICATION_GROUP_ID;

public class GitManager {
    private static final Logger log = Logger.getInstance(GitManager.class);

    private final SimpleGitProjectComponent plugin;
    private boolean repositoryChanging = false;
    private GitRepositoryManager repositoryManager;
    private ActionInfo myActionInfo;
    private ScopeInfo myScopeInfo;
    private Git git;

    public GitManager(SimpleGitProjectComponent plugin) {
        this.plugin = plugin;
        myScopeInfo = ScopeInfo.PROJECT;
        myActionInfo = ActionInfo.UPDATE;

        repositoryManager = ServiceManager.getService(plugin.getProject(), GitRepositoryManager.class);
        git = Git.getInstance();
        subscribeToRepoChangeEvents();
    }

    public void suggestRepository(SettingsProvider settings) {
        settings.setRemoteGitRepositoryURL("");
    }

    public List<GitRepository> getGitRepositories() {
        VirtualFile gitRoot = plugin.getProject().getBaseDir();
        return repositoryManager.getRepositories();
    }

    public Collection<GitRemote> getRemoteGitRepositories(GitRepository repository) {
        return repository.getRemotes();
    }

    public void synchronizeGit(AnActionEvent event) {
        GitSynchronizeAction.setStatus(SynchronizeStatus.UPDATING);
        updateProject(event);
    }

    private GitRepository getGitRepository() {
        VirtualFile gitRoot = plugin.getProject().getBaseDir();
        repositoryManager.getRepositories();
        return repositoryManager.getRepositoryForRoot(gitRoot);
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
            GitSynchronizeAction.setStatus(SynchronizeStatus.READY);
        }
    }

    public void afterUpdate(boolean updateSuccess) {
        if (updateSuccess)
            GitSynchronizeAction.setStatus(SynchronizeStatus.UPDATED);
        else
            GitSynchronizeAction.setStatus(SynchronizeStatus.READY);
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

    @CalledInAwt
    public void pushGit() {
        GitSynchronizeAction.setStatus(SynchronizeStatus.PUSHING);
        Project project = ObjectUtils.notNull(plugin.getProject());
        ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);

        String filePath = FeatureList.getInstance(project).getSelectedFeature().getPath();

        AbstractVcs abstractVcs = projectLevelVcsManager.getVcsFor(new LocalFilePath(filePath, false));
        GitPushSupport pushSupport = (GitPushSupport) DvcsUtil.getPushSupport(abstractVcs);

        String repositoryPath = plugin.getGitRepositoryRootPath();
        List<GitRepository> repositories = getGitRepositories();
        Optional<GitRepository> repository = repositories.stream()
                .filter(repo -> repo.getRoot().getPath().equals(repositoryPath))
                .findFirst();

        GitPushSource source = pushSupport.getSource(repository.get());
        GitPushTarget target = pushSupport.getDefaultTarget(repository.get());

        PushSpec<GitPushSource, GitPushTarget> pushSourceGitPushTargetPushSpec = new PushSpec<>(source, target);
        Map<GitRepository, PushSpec<GitPushSource, GitPushTarget>> pushSpecs =
                Collections.singletonMap(repository.get(), pushSourceGitPushTargetPushSpec);

        pushSupport.getPusher().push(pushSpecs, null, false);
        GitSynchronizeAction.setStatus(SynchronizeStatus.READY);
    }

    public void commit() {
        GitSynchronizeAction.setStatus(SynchronizeStatus.COMMITING);
        log.debug("Commit");
        Project project = ObjectUtils.notNull(plugin.getProject());

        ChangeListManager manager = ChangeListManager.getInstance(project);
//        LocalChangeList initialSelection = manager.getDefaultChangeList();
        String absolutePath = FeatureList.getInstance(project).getSelectedFeature().getPath();
        String relativePath = new File(plugin.getFeaturePath()).toURI().relativize(new File(absolutePath).toURI()).getPath();
        FilePath filePath = new LocalFilePath(absolutePath, false);

        Change change = manager.getChange(filePath);
        if (change != null) {
            List<Change> changesToCommit = Collections.singletonList(change);
            ProjectLevelVcsManager plvm = ProjectLevelVcsManager.getInstance(project);
            AbstractVcs vcs = plvm.getVcsFor(filePath);
            List<VcsException> exceptionList = vcs.getCheckinEnvironment().commit(changesToCommit, "Commit " + relativePath);
            VcsDirtyScopeManager.getInstance(project).filePathsDirty(ChangesUtil.getPaths(changesToCommit), null);
            if (exceptionList != null && exceptionList.size() > 0) {
                GitSynchronizeAction.setStatus(SynchronizeStatus.READY);
            } else {
                GitSynchronizeAction.setStatus(SynchronizeStatus.COMMITED);
            }
        } else {
            GitSynchronizeAction.setStatus(SynchronizeStatus.READY);
        }
    }

    public GitLocalBranch getCurrentBranch() {
        GitRepository repository = getGitRepository();
        return repository.getCurrentBranch();
    }

    public void checkoutExistingOrNewBranch(String branchName, Runnable callInAwtAfterExecution) {
        GitRepository repository = getGitRepository();
        GitLocalBranch branch = repository.getBranches().findLocalBranch(branchName);
        if (branch == null) {
            new CommonBackgroundTask(plugin.getProject(), "Checking out new branch " + branchName, this::pushGit) {
                @Override public void execute(@NotNull ProgressIndicator indicator) {
                    newWorker(indicator).checkoutNewBranch(branchName, Collections.singletonList(repository));
                }
            }.runInBackground();
        } else {
            new CommonBackgroundTask(plugin.getProject(), "Checking out new branch " + branchName, callInAwtAfterExecution) {
                @Override public void execute(@NotNull ProgressIndicator indicator) {
                    newWorker(indicator).checkout(branchName, false, Collections.singletonList(repository));
                }
            }.runInBackground();
        }
    }

    public GitLocalBranch getLocalBranchByRemoteName(String remoteBranchName) {
        GitRepository repository = getGitRepository();
        for (GitBranchTrackInfo track : repository.getBranchTrackInfos()) {
            if (track.getRemoteBranch().getName().equals(remoteBranchName)) {
                return track.getLocalBranch();
            }
        }
        return null;
    }

    private static abstract class CommonBackgroundTask extends Task.Backgroundable {

        @Nullable private final Runnable myCallInAwtAfterExecution;

        private CommonBackgroundTask(@Nullable final Project project, @NotNull final String title, @Nullable Runnable callInAwtAfterExecution) {
            super(project, title);
            myCallInAwtAfterExecution = callInAwtAfterExecution;
        }

        @Override
        public final void run(@NotNull ProgressIndicator indicator) {
            execute(indicator);
            if (myCallInAwtAfterExecution != null) {
                Application application = ApplicationManager.getApplication();
                if (application.isUnitTestMode()) {
                    myCallInAwtAfterExecution.run();
                }
                else {
                    application.invokeLater(myCallInAwtAfterExecution, application.getDefaultModalityState());
                }
            }
        }

        abstract void execute(@NotNull ProgressIndicator indicator);

        void runInBackground() {
            GitVcs.runInBackground(this);
        }

    }

    private GitBranchWorker newWorker(ProgressIndicator indicator) {
        Project project = plugin.getProject();
        return new GitBranchWorker(project, git, new GitBranchUiHandlerImpl(project, git, indicator));
    }
}
