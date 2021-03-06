package ru.lanit.ideaplugin.simplegit.git;

import com.intellij.CommonBundle;
import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.push.*;
import com.intellij.dvcs.push.ui.*;
import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.dvcs.ui.DvcsBundle;
import com.intellij.ide.util.DelegatingProgressIndicator;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.CalledInAny;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.intellij.openapi.ui.Messages.OK;

public class Pusher implements Disposable {
    @NotNull private final Project myProject;
    @NotNull private final List<? extends Repository> myPreselectedRepositories;
    @NotNull private final VcsRepositoryManager myGlobalRepositoryManager;
    @NotNull private final List<PushSupport<Repository, PushSource, PushTarget>> myPushSupports;
    @NotNull private final PushLog myPushLog;
    @NotNull private final PushSettings myPushSettings;
    @NotNull private final Set<String> myExcludedRepositoryRoots;
    @Nullable private final Repository myCurrentlyOpenedRepository;
    private final List<PrePushHandler> myHandlers = ContainerUtil.newArrayList();
    private final boolean mySingleRepoProject;
    private static final int DEFAULT_CHILDREN_PRESENTATION_NUMBER = 20;
    private final ExecutorService myExecutorService = ConcurrencyUtil.newSingleThreadExecutor("DVCS Push");

    private final Map<RepositoryNode, Pusher.MyRepoModel<?, ?, ?>> myView2Model = new TreeMap<>();
    private SimpleGitProjectComponent myPlugin;

    public Pusher(@NotNull SimpleGitProjectComponent plugin,
                  @NotNull List<? extends Repository> preselectedRepositories, @Nullable Repository currentRepo) {
        myPlugin = plugin;
        myProject = plugin.getProject();
        myPushSettings = ServiceManager.getService(myProject, PushSettings.class);
        ContainerUtil.addAll(myHandlers, PrePushHandler.EP_NAME.getExtensions(myProject));
        myGlobalRepositoryManager = VcsRepositoryManager.getInstance(myProject);
        myExcludedRepositoryRoots = ContainerUtil.newHashSet(myPushSettings.getExcludedRepoRoots());
        myPreselectedRepositories = preselectedRepositories;
        myCurrentlyOpenedRepository = currentRepo;
        myPushSupports = getAffectedSupports();
        mySingleRepoProject = isSingleRepoProject();
        CheckedTreeNode rootNode = new CheckedTreeNode(null);
        createTreeModel(rootNode);
        myPushLog = new PushLog(myProject, rootNode, isSyncStrategiesAllowed());
        startLoadingCommits();
    }

    private boolean isSyncStrategiesAllowed() {
        return !mySingleRepoProject &&
                ContainerUtil.and(getAffectedSupports(), support -> support.mayChangeTargetsSync());
    }

    private boolean isSingleRepoProject() {
        return myGlobalRepositoryManager.getRepositories().size() == 1;
    }

    @NotNull
    private <R extends Repository, S extends PushSource, T extends PushTarget> List<PushSupport<R, S, T>> getAffectedSupports() {
        Collection<Repository> repositories = myGlobalRepositoryManager.getRepositories();
        Collection<AbstractVcs> vcss = ContainerUtil.map2Set(repositories, repository -> repository.getVcs());
        return ContainerUtil.map(vcss, (Function<AbstractVcs, PushSupport<R, S, T>>)vcs -> {
            //noinspection unchecked
            return DvcsUtil.getPushSupport(vcs);
        });
    }

    public boolean isForcePushEnabled() {
        return ContainerUtil.exists(myView2Model.values(), model -> model.getSupport().isForcePushEnabled());
    }

    @Nullable
    public PushTarget getProhibitedTarget() {
        Pusher.MyRepoModel model = ContainerUtil.find(myView2Model.values(), (Condition<Pusher.MyRepoModel>) model1 -> {
            PushTarget target = model1.getTarget();
            return model1.isSelected() &&
                    target != null && !model1.getSupport().isForcePushAllowed(model1.getRepository(), target);
        });
        return model != null ? model.getTarget() : null;
    }

    private void startLoadingCommits() {
        Map<RepositoryNode, Pusher.MyRepoModel> priorityLoading = ContainerUtil.newLinkedHashMap();
        Map<RepositoryNode, Pusher.MyRepoModel> others = ContainerUtil.newLinkedHashMap();
        RepositoryNode nodeForCurrentEditor = findNodeByRepo(myCurrentlyOpenedRepository);
        for (Map.Entry<RepositoryNode, Pusher.MyRepoModel<?, ?, ?>> entry : myView2Model.entrySet()) {
            Pusher.MyRepoModel model = entry.getValue();
            Repository repository = model.getRepository();
            RepositoryNode repoNode = entry.getKey();
            if (preselectByUser(repository)) {
                priorityLoading.put(repoNode, model);
            }
            else if (model.getSupport().shouldRequestIncomingChangesForNotCheckedRepositories() && !repoNode.equals(nodeForCurrentEditor)) {
                others.put(repoNode, model);
            }
            if (shouldPreSelect(model)) {
                model.setChecked(true);
            }
        }
        if (nodeForCurrentEditor != null) {
            //add repo for currently opened editor to the end of priority queue
            priorityLoading.put(nodeForCurrentEditor, myView2Model.get(nodeForCurrentEditor));
        }
        loadCommitsFromMap(priorityLoading);
        loadCommitsFromMap(others);
    }

    private boolean shouldPreSelect(@NotNull Pusher.MyRepoModel model) {
        Repository repository = model.getRepository();
        return preselectByUser(repository) ||
                (notExcludedByUser(repository) && model.getSupport().shouldRequestIncomingChangesForNotCheckedRepositories());
    }

    private RepositoryNode findNodeByRepo(@Nullable final Repository repository) {
        if (repository == null) return null;
        Map.Entry<RepositoryNode, Pusher.MyRepoModel<?, ?, ?>> entry =
                ContainerUtil.find(myView2Model.entrySet(), entry1 -> {
                    Pusher.MyRepoModel model = entry1.getValue();
                    return model.getRepository().getRoot().equals(repository.getRoot());
                });
        return entry != null ? entry.getKey() : null;
    }

    private void loadCommitsFromMap(@NotNull Map<RepositoryNode, Pusher.MyRepoModel> items) {
        for (Map.Entry<RepositoryNode, Pusher.MyRepoModel> entry : items.entrySet()) {
            RepositoryNode node = entry.getKey();
            loadCommits(entry.getValue(), node, true);
        }
    }

    private void createTreeModel(@NotNull CheckedTreeNode rootNode) {
        for (Repository repository : DvcsUtil.sortRepositories(myGlobalRepositoryManager.getRepositories())) {
            createRepoNode(repository, rootNode);
        }
    }

    @Nullable
    private <R extends Repository, S extends PushSource, T extends PushTarget> PushSupport<R, S, T> getPushSupportByRepository(@NotNull final R repository) {
        //noinspection unchecked
        return (PushSupport<R, S, T>)ContainerUtil.find(
                myPushSupports,
                (Condition<PushSupport<? extends Repository, ? extends PushSource, ? extends PushTarget>>)support -> support.getVcs().equals(repository.getVcs()));
    }

    private <R extends Repository, S extends PushSource, T extends PushTarget> void createRepoNode(@NotNull final R repository,
                                                                                                   @NotNull final CheckedTreeNode rootNode) {

        PushSupport<R, S, T> support = getPushSupportByRepository(repository);
        if (support == null) return;

        T target = support.getDefaultTarget(repository);
        String repoName = getDisplayedRepoName(repository);
        S source = support.getSource(repository);
        final Pusher.MyRepoModel<R, S, T> model = new Pusher.MyRepoModel<>(repository, support, mySingleRepoProject,
                source, target);
        if (target == null) {
            model.setError(VcsError.createEmptyTargetError(repoName));
        }

        final PushTargetPanel<T> pushTargetPanel = support.createTargetPanel(repository, target);
        final RepositoryWithBranchPanel<T> repoPanel = new RepositoryWithBranchPanel<>(myProject, repoName,
                source.getPresentation(), pushTargetPanel);
        CheckBoxModel checkBoxModel = model.getCheckBoxModel();
        final RepositoryNode repoNode = mySingleRepoProject
                ? new SingleRepositoryNode(repoPanel, checkBoxModel)
                : new RepositoryNode(repoPanel, checkBoxModel, target != null);
        // TODO: Implement IDEA-136937, until that do not change below class to avoid breakage of Gerrit plugin
        // (https://github.com/uwolfer/gerrit-intellij-plugin/issues/275)
        //noinspection Convert2Lambda
        pushTargetPanel.setFireOnChangeAction(new Runnable() {
            @Override
            public void run() {
                repoPanel.fireOnChange();
                ((DefaultTreeModel)myPushLog.getTree().getModel()).nodeChanged(repoNode); // tell the tree to repaint the changed node
            }
        });
        myView2Model.put(repoNode, model);
        repoPanel.addRepoNodeListener(new RepositoryNodeListener<T>() {
            @Override
            public void onTargetChanged(T newTarget) {
                repoNode.setChecked(true);
                myExcludedRepositoryRoots.remove(model.getRepository().getRoot().getPath());
                if (!newTarget.equals(model.getTarget()) || model.hasError() || !model.hasCommitInfo()) {
                    model.setTarget(newTarget);
                    model.clearErrors();
                    loadCommits(model, repoNode, false);
                }
            }

            @Override
            public void onSelectionChanged(boolean isSelected) {
                //myDialog.updateOkActions();
                if (isSelected) {
                    boolean forceLoad = myExcludedRepositoryRoots.remove(model.getRepository().getRoot().getPath());
                    if (!model.hasCommitInfo() && (forceLoad || !model.getSupport().shouldRequestIncomingChangesForNotCheckedRepositories())) {
                        loadCommits(model, repoNode, false);
                    }
                }
                else {
                    myExcludedRepositoryRoots.add(model.getRepository().getRoot().getPath());
                }
            }

            @Override
            public void onTargetInEditMode(@NotNull String currentValue) {
                myPushLog.fireEditorUpdated(currentValue);
            }
        });
        rootNode.add(repoNode);
    }

    // TODO This logic shall be moved to some common place and used instead of DvcsUtil.getShortRepositoryName
    @NotNull
    private String getDisplayedRepoName(@NotNull Repository repository) {
        String name = DvcsUtil.getShortRepositoryName(repository);
        int slash = name.lastIndexOf(File.separatorChar);
        if (slash < 0) {
            return name;
        }
        String candidate = name.substring(slash + 1);
        return !containedInOtherNames(repository, candidate) ? candidate : name;
    }

    private boolean containedInOtherNames(@NotNull final Repository except, final String candidate) {
        return ContainerUtil.exists(myGlobalRepositoryManager.getRepositories(),
                repository -> !repository.equals(except) && repository.getRoot().getName().equals(candidate));
    }

    public boolean isPushAllowed(final boolean force) {
        JTree tree = myPushLog.getTree();
        return !tree.isEditing() &&
                ContainerUtil.exists(myPushSupports, support -> isPushAllowed(support, force));
    }

    private boolean isPushAllowed(@NotNull PushSupport<?, ?, ?> pushSupport, boolean force) {
        Collection<RepositoryNode> nodes = getNodesForSupport(pushSupport);
        if (hasSomethingToPush(nodes)) return true;
        if (hasCheckedNodesWithContent(nodes, force)) {
            return !pushSupport.getRepositoryManager().isSyncEnabled() || !hasLoadingNodes(nodes);
        }
        return false;
    }

    private boolean hasSomethingToPush(Collection<RepositoryNode> nodes) {
        return ContainerUtil.exists(nodes, node -> {
            PushTarget target = myView2Model.get(node).getTarget();
            //if node is selected target should not be null
            return node.isChecked() && target != null && target.hasSomethingToPush();
        });
    }

    private boolean hasCheckedNodesWithContent(@NotNull Collection<RepositoryNode> nodes, final boolean withRefs) {
        return ContainerUtil.exists(nodes, node -> node.isChecked() && (withRefs || !myView2Model.get(node).getLoadedCommits().isEmpty()));
    }

    @NotNull
    private Collection<RepositoryNode> getNodesForSupport(final PushSupport<?, ?, ?> support) {
        return ContainerUtil
                .mapNotNull(myView2Model.entrySet(), entry -> support.equals(entry.getValue().getSupport()) ? entry.getKey() : null);
    }

    private static boolean hasLoadingNodes(@NotNull Collection<RepositoryNode> nodes) {
        return ContainerUtil.exists(nodes, node -> node.isLoading());
    }

    private <R extends Repository, S extends PushSource, T extends PushTarget> void loadCommits(@NotNull final Pusher.MyRepoModel<R, S, T> model,
                                                                                                @NotNull final RepositoryNode node,
                                                                                                final boolean initial) {
        node.cancelLoading();
        final T target = model.getTarget();
        if (target == null) {
            node.stopLoading();
            return;
        }
        node.setEnabled(true);
        final PushSupport<R, S, T> support = model.getSupport();
        final AtomicReference<OutgoingResult> result = new AtomicReference<>();
        Runnable task = () -> {
            final R repository = model.getRepository();
            OutgoingResult outgoing = support.getOutgoingCommitsProvider()
                    .getOutgoingCommits(repository, new PushSpec<>(model.getSource(), model.getTarget()), initial);
            result.compareAndSet(null, outgoing);
            UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
                OutgoingResult outgoing1 = result.get();
                List<VcsError> errors = outgoing1.getErrors();
                boolean shouldBeSelected;
                if (!errors.isEmpty()) {
                    shouldBeSelected = false;
                    model.setLoadedCommits(ContainerUtil.emptyList());
                    myPushLog.setChildren(node, ContainerUtil.map(errors, (Function<VcsError, DefaultMutableTreeNode>)error -> {
                        VcsLinkedTextComponent errorLinkText = new VcsLinkedTextComponent(error.getText(), new VcsLinkListener() {
                            @Override
                            public void hyperlinkActivated(@NotNull DefaultMutableTreeNode sourceNode, @NotNull MouseEvent event) {
                                error.handleError(new CommitLoader() {
                                    @Override
                                    public void reloadCommits() {
                                        node.setChecked(true);
                                        loadCommits(model, node, false);
                                    }
                                });
                            }
                        });
                        return new TextWithLinkNode(errorLinkText);
                    }));
                    if (node.isChecked()) {
                        node.setChecked(false);
                    }
                }
                else {
                    List<? extends VcsFullCommitDetails> commits = outgoing1.getCommits();
                    model.setLoadedCommits(commits);
                    shouldBeSelected = shouldSelectNodeAfterLoad(model);
                    myPushLog.setChildren(node,
                            getPresentationForCommits(Pusher.this.myProject, model.getLoadedCommits(),
                                    model.getNumberOfShownCommits()));
                    if (!commits.isEmpty()) {
                        myPushLog.selectIfNothingSelected(node);
                    }
                }
                node.stopLoading();
                updateLoadingPanel();
                if (shouldBeSelected) {
                    node.setChecked(true);
                }
                else if (initial) {
                    //do not un-check if user checked manually and no errors occurred, only initial check may be changed
                    node.setChecked(false);
                }
//                myDialog.updateOkActions();
            });
        };
        node.startLoading(myPushLog.getTree(), myExecutorService.submit(task, result), initial);
        updateLoadingPanel();
    }

    private void updateLoadingPanel() {
        myPushLog.getTree().setPaintBusy(hasLoadingNodes(myView2Model.keySet()));
    }

    private boolean shouldSelectNodeAfterLoad(@NotNull Pusher.MyRepoModel model) {
        if (mySingleRepoProject) return true;
        return hasCommitsToPush(model) && model.isSelected();
    }

    private boolean notExcludedByUser(@NotNull Repository repository) {
        return !myExcludedRepositoryRoots.contains(repository.getRoot().getPath());
    }

    private boolean preselectByUser(@NotNull Repository repository) {
        return mySingleRepoProject || myPreselectedRepositories.contains(repository);
    }

    private static boolean hasCommitsToPush(@NotNull Pusher.MyRepoModel model) {
        PushTarget target = model.getTarget();
        assert target != null;
        return (!model.getLoadedCommits().isEmpty() || target.hasSomethingToPush());
    }

    public PushLog getPushPanelLog() {
        return myPushLog;
    }

    public static class HandlerException extends RuntimeException {

        private final String myHandlerName;

        public HandlerException(@NotNull String name, @NotNull Throwable cause) {
            super(cause);
            myHandlerName = name;
        }

        @NotNull
        public String getHandlerName() {
            return myHandlerName;
        }
    }

    private static class StepsProgressIndicator extends DelegatingProgressIndicator {
        private final int myTotalSteps;
        private final AtomicInteger myFinishedTasks = new AtomicInteger();

        public StepsProgressIndicator(@NotNull ProgressIndicator indicator, int totalSteps) {
            super(indicator);
            myTotalSteps = totalSteps;
        }

        public void nextStep() {
            myFinishedTasks.incrementAndGet();
            setFraction(0);
        }

        @Override
        public void setFraction(double fraction) {
            super.setFraction((myFinishedTasks.get() + fraction) / (double) myTotalSteps);
        }
    }

    @NotNull
    @CalledInAny
    public PrePushHandler.Result executeHandlers(@NotNull ProgressIndicator indicator) throws ProcessCanceledException, Pusher.HandlerException {
        if (myHandlers.isEmpty()) return PrePushHandler.Result.OK;
        List<PushInfo> pushDetails = preparePushDetails();
        Pusher.StepsProgressIndicator stepsIndicator = new Pusher.StepsProgressIndicator(indicator, myHandlers.size());
        stepsIndicator.setIndeterminate(false);
        stepsIndicator.setFraction(0);
        for (PrePushHandler handler : myHandlers) {
            stepsIndicator.checkCanceled();
            stepsIndicator.setText(handler.getPresentableName());
            PrePushHandler.Result prePushHandlerResult;
            try {
                prePushHandlerResult = handler.handle(pushDetails, stepsIndicator);
            }
            catch (ProcessCanceledException pce) {
                throw pce;
            }
            catch (Throwable e) {
                throw new Pusher.HandlerException(handler.getPresentableName(), e);
            }

            if (prePushHandlerResult != PrePushHandler.Result.OK) {
                return prePushHandlerResult;
            }
            //the handler could change an indeterminate flag
            stepsIndicator.setIndeterminate(false);
            stepsIndicator.nextStep();
        }
        return PrePushHandler.Result.OK;
    }

    public void push(final boolean force) {
        Task.Backgroundable task = new Task.Backgroundable(myProject, "Pushing...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                myPushSettings.saveExcludedRepoRoots(myExcludedRepositoryRoots);
                for (PushSupport support : myPushSupports) {
                    doPushSynchronously(support, force);
                }
                myPlugin.getGitManager().afterPush();
            }
        };
        task.queue();
    }

    private <R extends Repository, S extends PushSource, T extends PushTarget> void doPushSynchronously(@NotNull PushSupport<R, S, T> support,
                                                                                                        boolean force) {
        VcsPushOptionValue options = null;
        com.intellij.dvcs.push.Pusher<R, S, T> pusher = support.getPusher();
        Map<R, PushSpec<S, T>> specs = collectPushSpecsForVcs(support);
        if (!specs.isEmpty()) {
            pusher.push(specs, options, force);
        }
    }

    private static <R extends Repository, S extends PushSource, T extends PushTarget> List<? extends VcsFullCommitDetails> loadCommits(@NotNull Pusher.MyRepoModel<R, S, T> model) {
        PushSupport<R, S, T> support = model.getSupport();
        R repository = model.getRepository();
        S source = model.getSource();
        T target = model.getTarget();
        if (target == null) {
            return ContainerUtil.emptyList();
        }
        OutgoingCommitsProvider<R, S, T> outgoingCommitsProvider = support.getOutgoingCommitsProvider();
        return outgoingCommitsProvider.getOutgoingCommits(repository, new PushSpec<>(source, target), true).getCommits();
    }

    @NotNull
    private List<PushInfo> preparePushDetails() {
        List<PushInfo> allDetails = ContainerUtil.newArrayList();
        Collection<Pusher.MyRepoModel<?, ?, ?>> repoModels = getSelectedRepoNode();

        for (Pusher.MyRepoModel<?, ?, ?> model : repoModels) {
            PushTarget target = model.getTarget();
            if (target == null) {
                continue;
            }
            PushSpec<PushSource, PushTarget> pushSpec = new PushSpec<>(model.getSource(), target);

            List<VcsFullCommitDetails> loadedCommits = ContainerUtil.newArrayList();
            loadedCommits.addAll(model.getLoadedCommits());
            if (loadedCommits.isEmpty()) {
                //Note: loadCommits is cancellable - it tracks current thread's progress indicator under the hood!
                loadedCommits.addAll(loadCommits(model));
            }

            //sort commits in the time-ascending order
            Collections.reverse(loadedCommits);
            allDetails.add(new Pusher.PushInfoImpl(model.getRepository(), pushSpec, loadedCommits));
        }
        return Collections.unmodifiableList(allDetails);
    }

    @NotNull
    private <R extends Repository, S extends PushSource, T extends PushTarget> Map<R, PushSpec<S, T>> collectPushSpecsForVcs(@NotNull PushSupport<R, S, T> pushSupport) {
        Map<R, PushSpec<S, T>> pushSpecs = ContainerUtil.newHashMap();
        Collection<Pusher.MyRepoModel<?, ?, ?>> repositoriesInformation = getSelectedRepoNode();
        for (Pusher.MyRepoModel<?, ?, ?> repoModel : repositoriesInformation) {
            if (pushSupport.equals(repoModel.getSupport())) {
                //todo improve generics: unchecked casts
                T target = (T)repoModel.getTarget();
                if (target != null) {
                    pushSpecs.put((R)repoModel.getRepository(), new PushSpec<>((S)repoModel.getSource(), target));
                }
            }
        }
        return pushSpecs;
    }

    private Collection<Pusher.MyRepoModel<?, ?, ?>> getSelectedRepoNode() {
        if (mySingleRepoProject) {
            return myView2Model.values();
        }
        //return all selected despite a loading state;
        return ContainerUtil.mapNotNull(myView2Model.entrySet(),
                entry -> {
                    Pusher.MyRepoModel<?, ?, ?> model = entry.getValue();
                    return model.isSelected() &&
                            model.getTarget() != null ? model :
                            null;
                });
    }

    @Override
    public void dispose() {
        myExecutorService.shutdownNow();
    }

    @NotNull
    public Project getProject() {
        return myProject;
    }

    private void addMoreCommits(RepositoryNode repositoryNode) {
        Pusher.MyRepoModel<?, ?, ?> repoModel = myView2Model.get(repositoryNode);
        repoModel.increaseShownCommits();
        myPushLog.setChildren(repositoryNode,
                getPresentationForCommits(
                        myProject,
                        repoModel.getLoadedCommits(),
                        repoModel.getNumberOfShownCommits()
                ));
    }


    @NotNull
    private List<DefaultMutableTreeNode> getPresentationForCommits(@NotNull final Project project,
                                                                   @NotNull List<? extends VcsFullCommitDetails> commits,
                                                                   int commitsNum) {
        Function<VcsFullCommitDetails, DefaultMutableTreeNode> commitToNode = commit -> new CommitNode(project, commit);
        List<DefaultMutableTreeNode> childrenToShown = new ArrayList<>();
        for (int i = 0; i < commits.size(); ++i) {
            if (i >= commitsNum) {
                final VcsLinkedTextComponent moreCommitsLink = new VcsLinkedTextComponent("<a href='loadMore'>...</a>", new VcsLinkListener() {
                    @Override
                    public void hyperlinkActivated(@NotNull DefaultMutableTreeNode sourceNode, @NotNull MouseEvent event) {
                        TreeNode parent = sourceNode.getParent();
                        if (parent instanceof RepositoryNode) {
                            addMoreCommits((RepositoryNode)parent);
                        }
                    }
                });
                childrenToShown.add(new TextWithLinkNode(moreCommitsLink));
                break;
            }
            childrenToShown.add(commitToNode.fun(commits.get(i)));
        }
        return childrenToShown;
    }

    @NotNull
    public Map<PushSupport, VcsPushOptionsPanel> createAdditionalPanels() {
        Map<PushSupport, VcsPushOptionsPanel> result = ContainerUtil.newLinkedHashMap();
        for (PushSupport support : myPushSupports) {
            ContainerUtil.putIfNotNull(support, support.createOptionsPanel(), result);
        }
        return result;
    }

    public boolean ensureForcePushIsNeeded() {
        Collection<Pusher.MyRepoModel<?, ?, ?>> selectedNodes = getSelectedRepoNode();
        Pusher.MyRepoModel<?, ?, ?> selectedModel = ContainerUtil.getFirstItem(selectedNodes);
        if (selectedModel == null) return false;
        final PushSupport activePushSupport = selectedModel.getSupport();
        final PushTarget commonTarget = getCommonTarget(selectedNodes);
        if (commonTarget != null && activePushSupport.isSilentForcePushAllowed(commonTarget)) return true;
        return Messages.showOkCancelDialog(myProject, XmlStringUtil.wrapInHtml(DvcsBundle.message("push.force.confirmation.text",
                commonTarget != null
                        ? " to <b>" +
                        commonTarget.getPresentation() + "</b>"
                        : "")),
                "Force Push", "&Force Push",
                CommonBundle.getCancelButtonText(),
                Messages.getWarningIcon(),
                commonTarget != null ? new Pusher.MyDoNotAskOptionForPush(activePushSupport, commonTarget) : null) == OK;
    }

    @Nullable
    private static PushTarget getCommonTarget(@NotNull Collection<Pusher.MyRepoModel<?, ?, ?>> selectedNodes) {
        final PushTarget commonTarget = ObjectUtils.assertNotNull(ContainerUtil.getFirstItem(selectedNodes)).getTarget();
        return commonTarget != null && !ContainerUtil.exists(selectedNodes, model -> !commonTarget.equals(model.getTarget())) ? commonTarget : null;
    }

    private static class PushInfoImpl implements PushInfo {

        private final Repository myRepository;
        private final PushSpec<PushSource, PushTarget> myPushSpec;
        private final List<VcsFullCommitDetails> myCommits;

        private PushInfoImpl(@NotNull Repository repository,
                             @NotNull PushSpec<PushSource, PushTarget> spec,
                             @NotNull List<VcsFullCommitDetails> commits) {
            myRepository = repository;
            myPushSpec = spec;
            myCommits = commits;
        }

        @NotNull
        @Override
        public Repository getRepository() {
            return myRepository;
        }

        @NotNull
        @Override
        public PushSpec<PushSource, PushTarget> getPushSpec() {
            return myPushSpec;
        }

        @NotNull
        @Override
        public List<VcsFullCommitDetails> getCommits() {
            return myCommits;
        }
    }

    private static class MyRepoModel<Repo extends Repository, S extends PushSource, T extends PushTarget> {
        @NotNull private final Repo myRepository;
        @NotNull private final PushSupport<Repo, S, T> mySupport;
        @NotNull private final S mySource;
        @Nullable private T myTarget;
        @Nullable VcsError myTargetError;

        int myNumberOfShownCommits;
        @NotNull List<? extends VcsFullCommitDetails> myLoadedCommits = Collections.emptyList();
        @NotNull private final CheckBoxModel myCheckBoxModel;

        public MyRepoModel(@NotNull Repo repository,
                           @NotNull PushSupport<Repo, S, T> supportForRepo,
                           boolean isSelected, @NotNull S source, @Nullable T target) {
            myRepository = repository;
            mySupport = supportForRepo;
            myCheckBoxModel = new CheckBoxModel(isSelected);
            mySource = source;
            myTarget = target;
            myNumberOfShownCommits = DEFAULT_CHILDREN_PRESENTATION_NUMBER;
        }

        @NotNull
        public Repo getRepository() {
            return myRepository;
        }

        @NotNull
        public PushSupport<Repo, S, T> getSupport() {
            return mySupport;
        }

        @NotNull
        public S getSource() {
            return mySource;
        }

        @Nullable
        public T getTarget() {
            return myTarget;
        }

        public void setTarget(@Nullable T target) {
            myTarget = target;
        }

        public boolean isSelected() {
            return myCheckBoxModel.isChecked();
        }

        public void setError(@Nullable VcsError error) {
            myTargetError = error;
        }

        public void clearErrors() {
            myTargetError = null;
        }

        public boolean hasError() {
            return myTargetError != null;
        }

        public int getNumberOfShownCommits() {
            return myNumberOfShownCommits;
        }

        public void increaseShownCommits() {
            myNumberOfShownCommits *= 2;
        }

        @NotNull
        public List<? extends VcsFullCommitDetails> getLoadedCommits() {
            return myLoadedCommits;
        }

        public void setLoadedCommits(@NotNull List<? extends VcsFullCommitDetails> loadedCommits) {
            myLoadedCommits = loadedCommits;
        }

        public boolean hasCommitInfo() {
            return myTargetError != null || !myLoadedCommits.isEmpty();
        }

        @NotNull
        public CheckBoxModel getCheckBoxModel() {
            return myCheckBoxModel;
        }

        public void setChecked(boolean checked) {
            myCheckBoxModel.setChecked(checked);
        }
    }

    private static class MyDoNotAskOptionForPush implements DialogWrapper.DoNotAskOption {

        @NotNull private final PushSupport myActivePushSupport;
        @NotNull private final PushTarget myCommonTarget;

        public MyDoNotAskOptionForPush(@NotNull PushSupport support,
                                       @NotNull PushTarget target) {
            myActivePushSupport = support;
            myCommonTarget = target;
        }

        @Override
        public boolean isToBeShown() {
            return true;
        }

        @Override
        public void setToBeShown(boolean toBeShown, int exitCode) {
            if (!toBeShown && exitCode == OK) {
                myActivePushSupport.saveSilentForcePushTarget(myCommonTarget);
            }
        }

        @Override
        public boolean canBeHidden() {
            return true;
        }

        @Override
        public boolean shouldSaveOptionsOnCancel() {
            return false;
        }

        @NotNull
        @Override
        public String getDoNotShowMessage() {
            return "Don't warn about this target";
        }
    }
}
