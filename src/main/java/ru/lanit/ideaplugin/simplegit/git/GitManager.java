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
import git4idea.GitRemoteBranch;
import git4idea.GitStandardRemoteBranch;
import git4idea.push.GitPushSource;
import git4idea.push.GitPushSupport;
import git4idea.push.GitPushTarget;
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
        GitSynchronizeAction.setStatus(SynchronizeStatus.UPDATING);
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

    private static String getAllFilesAreUpToDateMessage(FilePath[] roots) {
        if (roots.length == 1 && !roots[0].isDirectory()) {
            return VcsBundle.message("message.text.file.is.up.to.date");
        }
        else {
            return VcsBundle.message("message.text.all.files.are.up.to.date");
        }
    }

    @NotNull
    private static Collection<Repository> collectRepositories(@NotNull VcsRepositoryManager vcsRepositoryManager,
                                                              @Nullable VirtualFile[] files) {
        if (files == null) return Collections.emptyList();
        Collection<Repository> repositories = ContainerUtil.newHashSet();
        for (VirtualFile file : files) {
            Repository repo = vcsRepositoryManager.getRepositoryForFile(file);
            if (repo != null) {
                repositories.add(repo);
            }
        }
        return repositories;
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
//        GitPushTarget target = new GitPushTarget(branch, false);

        PushSpec<GitPushSource, GitPushTarget> pushSourceGitPushTargetPushSpec = new PushSpec<>(source, target);
        Map<GitRepository, PushSpec<GitPushSource, GitPushTarget>> pushSpecs =
                Collections.singletonMap(repository.get(), pushSourceGitPushTargetPushSpec);

        pushSupport.getPusher().push(pushSpecs, null, false);
        GitSynchronizeAction.setStatus(SynchronizeStatus.READY);

//        Project project = plugin.getProject();
//        String repositoryPath = plugin.getGitRepositoryRootPath();
//        List<GitRepository> repositories = getGitRepositories();
//        Optional<GitRepository> repository = repositories.stream()
//                .filter(repo -> repo.getRoot().getPath().equals(repositoryPath))
//                .findFirst();
//
//        List<? extends Repository> selectedRepositories = DvcsUtil.sortRepositories(repositories);
//
//        if (repository.isPresent()) {
//            final Pusher myController = new Pusher(plugin, selectedRepositories, repository.get());
//            FileDocumentManager.getInstance().saveAllDocuments();
//            AtomicReference<PrePushHandler.Result> result = new AtomicReference<>(PrePushHandler.Result.OK);
//            new Task.Modal(project, "Checking Commits...", true) {
//                @Override
//                public void run(@NotNull ProgressIndicator indicator) {
//                    result.set(myController.executeHandlers(indicator));
//                }
//
//                @Override
//                public void onSuccess() {
//                    super.onSuccess();
//                    if (result.get() == PrePushHandler.Result.OK) {
//                        doPush();
//                    }
//                    else if (result.get() == PrePushHandler.Result.ABORT_AND_CLOSE) {
//                        afterPush();
//                    }
//                    else if (result.get() == PrePushHandler.Result.ABORT) {
//                        afterPush();
//                    }
//                }
//
//                private void doPush() {
//                    myController.push(false);
//                }
//
//                @Override
//                public void onThrowable(@NotNull Throwable error) {
//                    if (error instanceof PushController.HandlerException) {
//                        super.onThrowable(error.getCause());
//
//                        String handlerName = ((PushController.HandlerException)error).getHandlerName();
//                        suggestToSkipOrPush(handlerName + " has failed. See log for more details.\n" +
//                                "Would you like to skip pre-push checking and continue or cancel push completely?");
//                    } else {
//                        super.onThrowable(error);
//                    }
//                }
//
//                @Override
//                public void onCancel() {
//                    super.onCancel();
//                    suggestToSkipOrPush("Would you like to skip pre-push checking and continue or cancel push completely?");
//                }
//
//                private void suggestToSkipOrPush(@NotNull String message) {
//                    if (Messages.showOkCancelDialog(myProject,
//                            message,
//                            "Push",
//                            "&Push Anyway",
//                            "&Cancel",
//                            UIUtil.getWarningIcon()) == Messages.OK) {
//                        doPush();
//                    }
//                }
//            }.queue();
//        }
    }

    public void afterPush() {
        GitSynchronizeAction.setStatus(SynchronizeStatus.READY);
    }

    /**
     * Adds a new line of text to a file and adds/commits it
     *
     * @param file
     * @param repository
     * @param project
     * @throws IOException
     * @throws IOException
     */
    public static void editAndCommitFile(final File file, final git4idea.repo.GitRepository repository, final Project project) throws IOException {
        // edits file
        final VirtualFile readmeVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        FileUtil.writeToFile(file, "\nnew line", true);
        // adds and commits the change
        final LocalChangeListImpl localChangeList = LocalChangeListImpl.createEmptyChangeListImpl(project, "TestCommit", "12345");
        final ChangeListManagerImpl changeListManager = ChangeListManagerImpl.getInstanceImpl(project);
        VcsDirtyScopeManager.getInstance(project).markEverythingDirty();
        changeListManager.ensureUpToDate(false);
        changeListManager.addUnversionedFiles(localChangeList, ImmutableList.of(readmeVirtualFile));
        final Change change = changeListManager.getChange(LocalFileSystem.getInstance().findFileByIoFile(file));
        repository.getVcs().getCheckinEnvironment().commit(ImmutableList.of(change), "test commit");
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

//        if (ProjectLevelVcsManager.getInstance(project).isBackgroundVcsOperationRunning()) {
//            log.debug("Background operation is running. returning.");
////            GitSynchronizeAction.status = SynchronizeStatus.READY;
//        }
//        FilePath[] roots = prepareRootsForCommit(getRoots(project), project);
//        ApplicationManager.getApplication().invokeLater(() -> {
//            ChangeListManager.getInstance(project)
//                    .invokeAfterUpdate(() -> performCheckIn(project, roots), InvokeAfterUpdateMode.BACKGROUND_CANCELLABLE,
//                            VcsBundle.message("waiting.changelists.update.for.show.commit.dialog.message"), ModalityState.current());
//        }, ModalityState.stateForComponent());
    }

    @NotNull
    protected FilePath[] prepareRootsForCommit(@NotNull FilePath[] roots, @NotNull Project project) {
        ApplicationManager.getApplication().saveAll();
        return DescindingFilesFilter.filterDescindingFiles(roots, project);
    }

    protected void performCheckIn(@NotNull Project project, @NotNull FilePath[] roots) {
        log.debug("invoking commit dialog after update");
        ChangeListManager manager = ChangeListManager.getInstance(project);
        LocalChangeList initialSelection = manager.getDefaultChangeList();
        String filePath = FeatureList.getInstance(project).getSelectedFeature().getPath();
        Collection<Change> changesToCommit = Collections.singletonList(manager.getChange(
                new LocalFilePath(filePath, false)
        ));

        boolean result = Commiter.commitChanges(project, changesToCommit, initialSelection, null, "comment",
                new MyCommitResultHandler()
        );
        if (!result)
            GitSynchronizeAction.setStatus(SynchronizeStatus.READY);
    }

    static private class MyCommitResultHandler implements CommitResultHandler {
        @Override
        public void onSuccess(@NotNull String commitMessage) {
            GitSynchronizeAction.setStatus(SynchronizeStatus.COMMITED);
        }

        @Override
        public void onFailure() {
            GitSynchronizeAction.setStatus(SynchronizeStatus.READY);
        }
    }

    @NotNull
    protected FilePath[] getRoots(@NotNull Project project) {
        ProjectLevelVcsManager manager = ProjectLevelVcsManager.getInstance(project);

        return Stream.of(manager.getAllActiveVcss())
                .filter(vcs -> vcs.getCheckinEnvironment() != null)
                .flatMap(vcs -> Stream.of(manager.getRootsUnderVcs(vcs)))
                .map(VcsUtil::getFilePath)
                .toArray(FilePath[]::new);
    }
}
