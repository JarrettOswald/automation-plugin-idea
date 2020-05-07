package ru.lanit.ideaplugin.simplegit.git;

import com.intellij.history.Label;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.ide.errorTreeView.HotfixData;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.RemoteRevisionsCache;
import com.intellij.openapi.vcs.changes.VcsAnnotationRefresher;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.changes.committed.CommittedChangesCache;
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import com.intellij.openapi.vcs.update.*;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.WaitForProgressToShow;
import com.intellij.vcs.ViewUpdateInfoNotification;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

import java.io.File;
import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.notNullize;
import static com.intellij.openapi.util.text.StringUtil.pluralize;
import static com.intellij.openapi.vcs.VcsNotifier.STANDARD_NOTIFICATION;
import static com.intellij.util.ObjectUtils.notNull;

public class Updater extends Task.Backgroundable {
    private static final Logger log = Logger.getInstance(Updater.class);

    private final String LOCAL_HISTORY_ACTION = VcsBundle.message("local.history.update.from.vcs");
    private final static String SYNCHRONIZE_GIT_TITLE = "Synchronize with git";

    private final Project myProject;
    private final ProjectLevelVcsManagerEx myProjectLevelVcsManager;
    private UpdatedFiles myUpdatedFiles;
    private final FilePath[] myRoots;
    private final Map<AbstractVcs, Collection<FilePath>> myVcsToVirtualFiles;
    private final Map<HotfixData, List<VcsException>> myGroupedExceptions;
    private final List<UpdateSession> myUpdateSessions;
    private int myUpdateNumber;

    // vcs name, context object
    private final Map<AbstractVcs, SequentialUpdatesContext> myContextInfo;
    private final VcsDirtyScopeManager myDirtyScopeManager;

    private Label myBefore;
    private Label myAfter;
    private LocalHistoryAction myLocalHistoryAction;
    private ActionInfo myActionInfo;
    private SimpleGitProjectComponent myPlugin;

    Updater(final SimpleGitProjectComponent plugin, final FilePath[] roots, final Map<AbstractVcs, Collection<FilePath>> vcsToVirtualFiles) {
        this(plugin.getProject(), roots, vcsToVirtualFiles);
        myPlugin = plugin;
    }

    Updater(final Project project, final FilePath[] roots, final Map<AbstractVcs, Collection<FilePath>> vcsToVirtualFiles) {
        super(project, SYNCHRONIZE_GIT_TITLE, true, VcsConfiguration.getInstance(project).getUpdateOption());
        myProject = project;
        myProjectLevelVcsManager = ProjectLevelVcsManagerEx.getInstanceEx(project);
        myDirtyScopeManager = VcsDirtyScopeManager.getInstance(myProject);
        myRoots = roots;
        myVcsToVirtualFiles = vcsToVirtualFiles;

        myUpdatedFiles = UpdatedFiles.create();
        myGroupedExceptions = new HashMap<>();
        myUpdateSessions = new ArrayList<>();

        // create from outside without any context; context is created by vcses
        myContextInfo = new HashMap<>();
        myUpdateNumber = 1;
        myActionInfo = ActionInfo.UPDATE;
    }

    private void reset() {
        myUpdatedFiles = UpdatedFiles.create();
        myGroupedExceptions.clear();
        myUpdateSessions.clear();
        ++myUpdateNumber;
    }

    @Override
    public void run(@NotNull final ProgressIndicator indicator) {
        runImpl();
    }

    private void runImpl() {
        ProjectManagerEx.getInstanceEx().blockReloadingProjectOnExternalChanges();
        myProjectLevelVcsManager.startBackgroundVcsOperation();

        myBefore = LocalHistory.getInstance().putSystemLabel(myProject, "Before update");
        myLocalHistoryAction = LocalHistory.getInstance().startAction(LOCAL_HISTORY_ACTION);
        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();

        try {
            int toBeProcessed = myVcsToVirtualFiles.size();
            int processed = 0;
            for (AbstractVcs vcs : myVcsToVirtualFiles.keySet()) {
                final UpdateEnvironment updateEnvironment = myActionInfo.getEnvironment(vcs);
                updateEnvironment.fillGroups(myUpdatedFiles);
                Collection<FilePath> files = myVcsToVirtualFiles.get(vcs);

                final SequentialUpdatesContext context = myContextInfo.get(vcs);
                final Ref<SequentialUpdatesContext> refContext = new Ref<>(context);

                // actual update
                UpdateSession updateSession =
                        updateEnvironment.updateDirectories(files.toArray(new FilePath[files.size()]), myUpdatedFiles, progressIndicator, refContext);

                myContextInfo.put(vcs, refContext.get());
                processed++;
                if (progressIndicator != null) {
                    progressIndicator.setFraction((double) processed / (double) toBeProcessed);
                    progressIndicator.setText2("");
                }
                final List<VcsException> exceptionList = updateSession.getExceptions();
                gatherExceptions(vcs, exceptionList);
                myUpdateSessions.add(updateSession);
            }
        } finally {
            try {
                ProgressManager.progress(VcsBundle.message("progress.text.synchronizing.files"));
                doVfsRefresh();
            } finally {
                myProjectLevelVcsManager.stopBackgroundVcsOperation();
                if (!myProject.isDisposed()) {
                    myProject.getMessageBus().syncPublisher(UpdatedFilesListener.UPDATED_FILES).
                            consume(UpdatedFilesReverseSide.getPathsFromUpdatedFiles(myUpdatedFiles));
                }
            }
        }
    }

    private void gatherExceptions(final AbstractVcs vcs, final List<VcsException> exceptionList) {
        final VcsExceptionsHotFixer fixer = vcs.getVcsExceptionsHotFixer();
        if (fixer == null) {
            putExceptions(null, exceptionList);
        } else {
            putExceptions(fixer.groupExceptions(ActionType.update, exceptionList));
        }
    }

    private void putExceptions(final Map<HotfixData, List<VcsException>> map) {
        for (Map.Entry<HotfixData, List<VcsException>> entry : map.entrySet()) {
            putExceptions(entry.getKey(), entry.getValue());
        }
    }

    private void putExceptions(final HotfixData key, @NotNull final List<VcsException> list) {
        if (list.isEmpty()) return;
        myGroupedExceptions.computeIfAbsent(key, k -> new ArrayList<>()).addAll(list);
    }

    private void doVfsRefresh() {
        System.out.println("Calling refresh files after update for roots: " + Arrays.toString(myRoots));
        RefreshVFsSynchronously.updateAllChanged(myUpdatedFiles);
        notifyAnnotations();
    }

    private void notifyAnnotations() {
        final VcsAnnotationRefresher refresher = myProject.getMessageBus().syncPublisher(VcsAnnotationRefresher.LOCAL_CHANGES_CHANGED);
        UpdateFilesHelper.iterateFileGroupFilesDeletedOnServerFirst(myUpdatedFiles, new UpdateFilesHelper.Callback() {
            @Override
            public void onFile(String filePath, String groupId) {
                refresher.dirty(filePath);
            }
        });
    }

    @NotNull
    private Notification prepareNotification(@NotNull UpdateInfoTree tree, boolean someSessionWasCancelled) {
        int allFiles = getUpdatedFilesCount();

        String title;
        String content;
        NotificationType type;
        if (someSessionWasCancelled) {
            title = "Project Partially Updated";
            content = allFiles + " " + pluralize("file", allFiles) + " updated";
            type = NotificationType.WARNING;
        }
        else {
            title = allFiles + " Project " + pluralize("File", allFiles) + " Updated";
            content = notNullize(prepareScopeUpdatedText(tree));
            type = NotificationType.INFORMATION;
        }

        return STANDARD_NOTIFICATION.createNotification(title, content, type, null);
    }

    private int getUpdatedFilesCount() {
        return myUpdatedFiles.getTopLevelGroups().stream().mapToInt(this::getFilesCount).sum();
    }

    private int getFilesCount(@NotNull FileGroup group) {
        return group.getFiles().size() + group.getChildren().stream().mapToInt(this::getFilesCount).sum();
    }

    @Nullable
    private String prepareScopeUpdatedText(@NotNull UpdateInfoTree tree) {
            /*String scopeText = null;
            NamedScope scopeFilter = tree.getFilterScope();
            if (scopeFilter != null) {
                int filteredFiles = tree.getFilteredFilesCount();
                String filterName = scopeFilter.getName();
                if (filteredFiles == 0) {
                    scopeText = filterName + " wasn't modified";
                }
                else {
                    scopeText = filteredFiles + " in " + filterName;
                }
            }
            return scopeText;*/
        return null;
    }

    @Override
    public void onSuccess() {
        onSuccessImpl(false);
    }

    private void onSuccessImpl(final boolean wasCanceled) {
        if (!myProject.isOpen() || myProject.isDisposed()) {
            ProjectManagerEx.getInstanceEx().unblockReloadingProjectOnExternalChanges();
            LocalHistory.getInstance().putSystemLabel(myProject, LOCAL_HISTORY_ACTION); // TODO check why this label is needed
            return;
        }
        boolean continueChain = false;
        for (SequentialUpdatesContext context : myContextInfo.values()) {
            continueChain |= (context != null) && (context.shouldFail());
        }
        final boolean continueChainFinal = continueChain;

        final boolean someSessionWasCancelled = wasCanceled || GitManager.someSessionWasCanceled(myUpdateSessions);
        // here text conflicts might be interactively resolved
        for (final UpdateSession updateSession : myUpdateSessions) {
            updateSession.onRefreshFilesCompleted();
        }
        // only after conflicts are resolved, put a label
        if (myLocalHistoryAction != null) {
            myLocalHistoryAction.finish();
        }
        myAfter = LocalHistory.getInstance().putSystemLabel(myProject, "After update");

        if (myActionInfo.canChangeFileStatus()) {
            final List<VirtualFile> files = new ArrayList<>();
            final RemoteRevisionsCache revisionsCache = RemoteRevisionsCache.getInstance(myProject);
            revisionsCache.invalidate(myUpdatedFiles);
            UpdateFilesHelper.iterateFileGroupFiles(myUpdatedFiles, new UpdateFilesHelper.Callback() {
                @Override
                public void onFile(final String filePath, final String groupId) {
                    @NonNls final String path = VfsUtilCore.pathToUrl(filePath.replace(File.separatorChar, '/'));
                    final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(path);
                    if (file != null) {
                        files.add(file);
                    }
                }
            });
            myDirtyScopeManager.filesDirty(files, null);
        }

        final boolean updateSuccess = !someSessionWasCancelled && myGroupedExceptions.isEmpty();

        WaitForProgressToShow.runOrInvokeLaterAboveProgress(() -> {
            if (myProject.isDisposed()) {
                ProjectManagerEx.getInstanceEx().unblockReloadingProjectOnExternalChanges();
                return;
            }

            if (!myGroupedExceptions.isEmpty()) {
                if (continueChainFinal) {
                    gatherContextInterruptedMessages();
                }
                AbstractVcsHelper.getInstance(myProject).showErrors(myGroupedExceptions, VcsBundle.message("message.title.vcs.update.errors",
                        SYNCHRONIZE_GIT_TITLE));
            }
            else if (someSessionWasCancelled) {
                ProgressManager.progress(VcsBundle.message("progress.text.updating.canceled"));
            }
            else {
                ProgressManager.progress(VcsBundle.message("progress.text.updating.done"));
            }

            final boolean noMerged = myUpdatedFiles.getGroupById(FileGroup.MERGED_WITH_CONFLICT_ID).isEmpty();
            if (myUpdatedFiles.isEmpty() && myGroupedExceptions.isEmpty()) {
                NotificationType type;
                String content;
                if (someSessionWasCancelled) {
                    content = VcsBundle.message("progress.text.updating.canceled");
                    type = NotificationType.WARNING;
                    VcsNotifier.getInstance(myProject).notify(STANDARD_NOTIFICATION.createNotification(content, type));
                }
            }
            else if (!myUpdatedFiles.isEmpty()) {
                final UpdateInfoTree tree = showUpdateTree(continueChainFinal && updateSuccess && noMerged, someSessionWasCancelled);
                final CommittedChangesCache cache = CommittedChangesCache.getInstance(myProject);
                cache.processUpdatedFiles(myUpdatedFiles, tree::setChangeLists);

                Notification notification = prepareNotification(tree, someSessionWasCancelled);
                notification.addAction(new ViewUpdateInfoNotification(myProject, tree, "View", notification));
                VcsNotifier.getInstance(myProject).notify(notification);
            }

            ProjectManagerEx.getInstanceEx().unblockReloadingProjectOnExternalChanges();

            if (continueChainFinal && updateSuccess) {
                if (!noMerged) {
                    showContextInterruptedError();
                }
                else {
                    // trigger next update; for CVS when updating from several branches simultaneously
                    reset();
                    ProgressManager.getInstance().run(this);
                }
            }
            myPlugin.getGitManager().afterUpdate(updateSuccess);
        }, null, myProject);
    }


    private void showContextInterruptedError() {
        gatherContextInterruptedMessages();
        AbstractVcsHelper.getInstance(myProject).showErrors(myGroupedExceptions,
                VcsBundle.message("message.title.vcs.update.errors", SYNCHRONIZE_GIT_TITLE));
    }

    private void gatherContextInterruptedMessages() {
        for (Map.Entry<AbstractVcs, SequentialUpdatesContext> entry : myContextInfo.entrySet()) {
            final SequentialUpdatesContext context = entry.getValue();
            if ((context == null) || (! context.shouldFail())) continue;
            final VcsException exception = new VcsException(context.getMessageWhenInterruptedBeforeStart());
            gatherExceptions(entry.getKey(), Collections.singletonList(exception));
        }
    }

    @NotNull
    private UpdateInfoTree showUpdateTree(final boolean willBeContinued, final boolean wasCanceled) {
        RestoreUpdateTree restoreUpdateTree = RestoreUpdateTree.getInstance(myProject);
        restoreUpdateTree.registerUpdateInformation(myUpdatedFiles, myActionInfo);
        final String text = SYNCHRONIZE_GIT_TITLE + ((willBeContinued || (myUpdateNumber > 1)) ? ("#" + myUpdateNumber) : "");
        UpdateInfoTree updateInfoTree = notNull(myProjectLevelVcsManager.showUpdateProjectInfo(myUpdatedFiles, text, myActionInfo,
                wasCanceled));
        updateInfoTree.setBefore(myBefore);
        updateInfoTree.setAfter(myAfter);
        updateInfoTree.setCanGroupByChangeList(canGroupByChangelist(myVcsToVirtualFiles.keySet()));
        return updateInfoTree;
    }

    @Override
    public void onCancel() {
        onSuccessImpl(true);
    }

    private boolean canGroupByChangelist(final Set<AbstractVcs> abstractVcses) {
        if (myActionInfo.canGroupByChangelist()) {
            for(AbstractVcs vcs: abstractVcses) {
                if (vcs.getCachingCommittedChangesProvider() != null) {
                    return true;
                }
            }
        }
        return false;
    }
}