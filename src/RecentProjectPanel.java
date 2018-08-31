/**
 * 拷贝自package com.intellij.openapi.wm.impl.welcomeScreen.RecentProjectPanel.java， 修改
 * Created by RS on 2018/8/31.
 */

import com.intellij.icons.AllIcons.Welcome.Project;
import com.intellij.ide.PowerSaveMode;
import com.intellij.ide.ProjectGroup;
import com.intellij.ide.ProjectGroupActionGroup;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.ide.PowerSaveMode.Listener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.UniqueNameBuilder;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.impl.welcomeScreen.BottomLineBorder;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeScreenColors;
import com.intellij.ui.ClickListener;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.speedSearch.ListWithFilter;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.JBUI.Fonts;
import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecentProjectPanel extends JPanel {
    private static final Color BORDER_COLOR = new JBColor(Gray._190, Gray._85);
    private static final Color CAPTION_BACKGROUND = new JBColor(Gray._210, Gray._75);
    private static final Color CAPTION_FOREGROUND = new JBColor(Color.black, Gray._197);

    public static final String RECENT_PROJECTS_LABEL = "Recent Projects";
    protected final JBList myList;
    protected final UniqueNameBuilder<ReopenProjectAction> myPathShortener;
    protected AnAction removeRecentProjectAction;
    private int myHoverIndex;
    private final int closeButtonInset;
    private Icon currentIcon;
    private static final Logger LOG = Logger.getInstance(RecentProjectPanel.class);
    Set<ReopenProjectAction> projectsWithLongPathes;
    private final JPanel myCloseButtonForEditor;
    protected RecentProjectPanel.FilePathChecker myChecker;

    private boolean rectInListCoordinatesContains(Rectangle listCellBounds, Point p) {
        int realCloseButtonInset = UIUtil.isJreHiDPI(this) ? (int)((float)this.closeButtonInset * JBUI.sysScale(this)) : this.closeButtonInset;
        Rectangle closeButtonRect = new Rectangle(this.myCloseButtonForEditor.getX() - realCloseButtonInset, this.myCloseButtonForEditor.getY() - realCloseButtonInset, this.myCloseButtonForEditor.getWidth() + realCloseButtonInset * 2, this.myCloseButtonForEditor.getHeight() + realCloseButtonInset * 2);
        Rectangle rectInListCoordinates = new Rectangle(new Point(closeButtonRect.x + listCellBounds.x, closeButtonRect.y + listCellBounds.y), closeButtonRect.getSize());
        return rectInListCoordinates.contains(p);
    }

//    public RecentProjectPanel(@NotNull Disposable parentDisposable) {
    private OpenRecentProjectAction.OnItemClickListener itemClickListener;
    public RecentProjectPanel(OpenRecentProjectAction.OnItemClickListener itemClickListener) {
        super(new BorderLayout());
        this.itemClickListener = itemClickListener;
        this.myHoverIndex = -1;
        this.closeButtonInset = JBUI.scale(7);
        this.currentIcon = Project.Remove;
        this.projectsWithLongPathes = new HashSet(0);
        this.myCloseButtonForEditor = new JPanel() {
            {
                this.setPreferredSize(new Dimension(RecentProjectPanel.this.currentIcon.getIconWidth(), RecentProjectPanel.this.currentIcon.getIconHeight()));
                this.setOpaque(true);
            }

            protected void paintComponent(Graphics g) {
                RecentProjectPanel.this.currentIcon.paintIcon(this, g, 0, 0);
            }
        };
        AnAction[] recentProjectActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false, this.isUseGroups());
        this.myPathShortener = new UniqueNameBuilder(SystemProperties.getUserHome(), File.separator, 40);
        Collection<String> pathsToCheck = ContainerUtil.newHashSet();
        AnAction[] var4 = recentProjectActions;
        int var5 = recentProjectActions.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            AnAction action = var4[var6];
            if (action instanceof ReopenProjectAction) {
                ReopenProjectAction item = (ReopenProjectAction)action;
                this.myPathShortener.addPath(item, item.getProjectPath());
                pathsToCheck.add(item.getProjectPath());
            }
        }

        if (Registry.is("autocheck.availability.welcome.screen.projects")) {
            this.myChecker = new RecentProjectPanel.FilePathChecker(new Runnable() {
                public void run() {
                    if (RecentProjectPanel.this.myList.isShowing()) {
                        RecentProjectPanel.this.myList.revalidate();
                        RecentProjectPanel.this.myList.repaint();
                    }

                }
            }, pathsToCheck);
//            Disposer.register(parentDisposable, this.myChecker);
        }

        this.myList = this.createList(recentProjectActions, this.getPreferredScrollableViewportSize());
        this.myList.setCellRenderer(this.createRenderer(this.myPathShortener));
        (new ClickListener() {
            public boolean onClick(@NotNull MouseEvent event, int clickCount) {
                if (event == null) {
//                    $$$reportNull$$$0(0);
                }

                int selectedIndex = RecentProjectPanel.this.myList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    Rectangle cellBounds = RecentProjectPanel.this.myList.getCellBounds(selectedIndex, selectedIndex);
                    if (cellBounds.contains(event.getPoint())) {
                        Object selection = RecentProjectPanel.this.myList.getSelectedValue();
                        if (Registry.is("removable.welcome.screen.projects") && RecentProjectPanel.this.rectInListCoordinatesContains(cellBounds, event.getPoint())) {
                            RecentProjectPanel.this.removeRecentProjectAction.actionPerformed((AnActionEvent)null);
                        } else if (selection != null) {
                            AnAction selectedAction = (AnAction)selection;
                            AnActionEvent actionEvent = AnActionEvent.createFromInputEvent(selectedAction, event, "WelcomeScreen");
                            selectedAction.actionPerformed(actionEvent);
                            if (selectedAction instanceof ReopenProjectAction && ((ReopenProjectAction)selectedAction).isRemoved()) {
                                ListUtil.removeSelectedItems(RecentProjectPanel.this.myList);
                            }
                            if (itemClickListener != null)
                                itemClickListener.onItemClick(selectedIndex);
                        }
                    }
                }

                return true;
            }
        }).installOn(this.myList);
        this.myList.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object[] selectedValued = RecentProjectPanel.this.myList.getSelectedValues();
                if (selectedValued != null) {
                    Object[] var3 = selectedValued;
                    int var4 = selectedValued.length;

                    for(int var5 = 0; var5 < var4; ++var5) {
                        Object selection = var3[var5];
                        AnActionEvent event = AnActionEvent.createFromInputEvent((AnAction)selection, (InputEvent)null, "WelcomeScreen");
                        ((AnAction)selection).actionPerformed(event);
                    }
                }

            }
        }, KeyStroke.getKeyStroke(10, 0), 1);
        this.removeRecentProjectAction = new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                Object[] selection = RecentProjectPanel.this.myList.getSelectedValues();
                if (selection != null && selection.length > 0) {
                    int rc = Messages.showOkCancelDialog(RecentProjectPanel.this, "Remove '" + StringUtil.join(selection, (action) -> {
                        return ((AnAction)action).getTemplatePresentation().getText();
                    }, "'\n'") + "' from recent projects list?", "Remove Recent Project", Messages.getQuestionIcon());
                    if (rc == 0) {
                        Object[] var4 = selection;
                        int var5 = selection.length;

                        for(int var6 = 0; var6 < var5; ++var6) {
                            Object projectAction = var4[var6];
                            RecentProjectPanel.removeRecentProjectElement(projectAction);
                        }

                        ListUtil.removeSelectedItems(RecentProjectPanel.this.myList);
                    }
                }

            }

            public void update(@NotNull AnActionEvent e) {
                if (e == null) {
//                    $$$reportNull$$$0(0);
                }

                e.getPresentation().setEnabled(true);
            }
        };
//        this.removeRecentProjectAction.registerCustomShortcutSet(CustomShortcutSet.fromString(new String[]{"DELETE", "BACK_SPACE"}), this.myList, parentDisposable);
        this.addMouseMotionListener();
        this.myList.setSelectedIndex(0);
        JBScrollPane scroll = new JBScrollPane(this.myList);
        scroll.setBorder((Border)null);
        JComponent list = recentProjectActions.length == 0 ? this.myList : ListWithFilter.wrap(this.myList, scroll, (o) -> {
            if (o instanceof ReopenProjectAction) {
                ReopenProjectAction item = (ReopenProjectAction)o;
                String home = SystemProperties.getUserHome();
                String path = item.getProjectPath();
                if (FileUtil.startsWith(path, home)) {
                    path = path.substring(home.length());
                }

                return item.getProjectName() + " " + path;
            } else {
                return o instanceof ProjectGroupActionGroup ? ((ProjectGroupActionGroup)o).getGroup().getName() : o.toString();
            }
        });
        this.add((Component)list, "Center");
        JPanel title = this.createTitle();
        if (title != null) {
            this.add(title, "North");
        }

        this.setBorder(new LineBorder(BORDER_COLOR));
    }

    protected boolean isPathValid(String path) {
        return this.myChecker == null || this.myChecker.isValid(path);
    }

    protected static void removeRecentProjectElement(Object element) {
        RecentProjectsManager manager = RecentProjectsManager.getInstance();
        if (element instanceof ReopenProjectAction) {
            manager.removePath(((ReopenProjectAction)element).getProjectPath());
        } else if (element instanceof ProjectGroupActionGroup) {
            ProjectGroup group = ((ProjectGroupActionGroup)element).getGroup();
            Iterator var3 = group.getProjects().iterator();

            while(var3.hasNext()) {
                String path = (String)var3.next();
                manager.removePath(path);
            }

            manager.removeGroup(group);
        }

    }

    protected boolean isUseGroups() {
        return false;
    }

    protected Dimension getPreferredScrollableViewportSize() {
        return JBUI.size(250, 400);
    }

    protected void addMouseMotionListener() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            boolean myIsEngaged = false;

            public void mouseMoved(MouseEvent e) {
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner == null) {
                    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
                        IdeFocusManager.getGlobalInstance().requestFocus(RecentProjectPanel.this.myList, true);
                    });
                }

                if (RecentProjectPanel.this.myList.getSelectedIndices().length <= 1) {
                    if (this.myIsEngaged && !UIUtil.isSelectionButtonDown(e) && !(focusOwner instanceof JRootPane)) {
                        Point point = e.getPoint();
                        int index = RecentProjectPanel.this.myList.locationToIndex(point);
                        RecentProjectPanel.this.myList.setSelectedIndex(index);
                        Rectangle cellBounds = RecentProjectPanel.this.myList.getCellBounds(index, index);
                        if (cellBounds != null && cellBounds.contains(point)) {
                            UIUtil.setCursor(RecentProjectPanel.this.myList, Cursor.getPredefinedCursor(12));
                            if (RecentProjectPanel.this.rectInListCoordinatesContains(cellBounds, point)) {
                                RecentProjectPanel.this.currentIcon = Project.Remove_hover;
                            } else {
                                RecentProjectPanel.this.currentIcon = Project.Remove;
                            }

                            RecentProjectPanel.this.myHoverIndex = index;
                            RecentProjectPanel.this.myList.repaint(cellBounds);
                        } else {
                            UIUtil.setCursor(RecentProjectPanel.this.myList, Cursor.getPredefinedCursor(0));
                            RecentProjectPanel.this.myHoverIndex = -1;
                            RecentProjectPanel.this.myList.repaint();
                        }
                    } else {
                        this.myIsEngaged = true;
                    }

                }
            }

            public void mouseExited(MouseEvent e) {
                RecentProjectPanel.this.myHoverIndex = -1;
                RecentProjectPanel.this.currentIcon = Project.Remove;
                RecentProjectPanel.this.myList.repaint();
            }
        };
        this.myList.addMouseMotionListener(mouseAdapter);
        this.myList.addMouseListener(mouseAdapter);
    }

    protected JBList createList(AnAction[] recentProjectActions, Dimension size) {
        return new RecentProjectPanel.MyList(this, size, recentProjectActions);
    }

    protected ListCellRenderer createRenderer(UniqueNameBuilder<ReopenProjectAction> pathShortener) {
        return new RecentProjectPanel.RecentProjectItemRenderer(pathShortener);
    }

    @Nullable
    protected JPanel createTitle() {
        JPanel title = new JPanel() {
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, JBUI.scale(28));
            }
        };
        title.setBorder(new BottomLineBorder());
        JLabel titleLabel = new JLabel("Recent Projects");
        title.add(titleLabel);
        titleLabel.setHorizontalAlignment(0);
        titleLabel.setForeground(CAPTION_FOREGROUND);
        title.setBackground(CAPTION_BACKGROUND);
        return title;
    }

    private static boolean isFileAvailable(File file) {
        List<File> roots = Arrays.asList(File.listRoots());

        for(File tmp = file; tmp != null; tmp = tmp.getParentFile()) {
            if (roots.contains(tmp)) {
                return file.exists();
            }
        }

        return false;
    }

    private static class FilePathChecker implements Disposable, ApplicationActivationListener, Listener {
        private static final int MIN_AUTO_UPDATE_MILLIS = 2500;
        private ScheduledExecutorService myService = null;
        private final Set<String> myInvalidPaths = Collections.synchronizedSet(new HashSet());
        private final Runnable myCallback;
        private final Collection<String> myPaths;

        FilePathChecker(Runnable callback, Collection<String> paths) {
            this.myCallback = callback;
            this.myPaths = paths;
            MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(this);
            connection.subscribe(ApplicationActivationListener.TOPIC, this);
            connection.subscribe(PowerSaveMode.TOPIC, this);
            this.onAppStateChanged();
        }

        boolean isValid(String path) {
            return !this.myInvalidPaths.contains(path);
        }

        public void applicationActivated(IdeFrame ideFrame) {
            this.onAppStateChanged();
        }

        public void delayedApplicationDeactivated(IdeFrame ideFrame) {
            this.onAppStateChanged();
        }

        public void applicationDeactivated(IdeFrame ideFrame) {
        }

        public void powerSaveStateChanged() {
            this.onAppStateChanged();
        }

        private void onAppStateChanged() {
            boolean settingsAreOK = Registry.is("autocheck.availability.welcome.screen.projects") && !PowerSaveMode.isEnabled();
            boolean everythingIsOK = settingsAreOK && ApplicationManager.getApplication().isActive();
            if (this.myService == null && everythingIsOK) {
                this.myService = AppExecutorUtil.createBoundedScheduledExecutorService("CheckRecentProjectPaths Service", 2);
                Iterator var3 = this.myPaths.iterator();

                while(var3.hasNext()) {
                    String path = (String)var3.next();
                    this.scheduleCheck(path, 0L);
                }

                ApplicationManager.getApplication().invokeLater(this.myCallback);
            }

            if (this.myService != null && !everythingIsOK) {
                if (!settingsAreOK) {
                    this.myInvalidPaths.clear();
                }

                if (!this.myService.isShutdown()) {
                    this.myService.shutdown();
                    this.myService = null;
                }

                ApplicationManager.getApplication().invokeLater(this.myCallback);
            }

        }

        public void dispose() {
            if (this.myService != null) {
                this.myService.shutdownNow();
            }

        }

        private void scheduleCheck(String path, long delay) {
            if (this.myService != null && !this.myService.isShutdown()) {
                this.myService.schedule(() -> {
                    long startTime = System.currentTimeMillis();

                    boolean pathIsValid;
                    try {
                        pathIsValid = RecentProjectPanel.isFileAvailable(new File(path));
                    } catch (Exception var6) {
                        pathIsValid = false;
                    }

                    if (this.myInvalidPaths.contains(path) == pathIsValid) {
                        if (pathIsValid) {
                            this.myInvalidPaths.remove(path);
                        } else {
                            this.myInvalidPaths.add(path);
                        }

                        ApplicationManager.getApplication().invokeLater(this.myCallback);
                    }

                    this.scheduleCheck(path, Math.max(2500L, 10L * (System.currentTimeMillis() - startTime)));
                }, delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    protected class RecentProjectItemRenderer extends JPanel implements ListCellRenderer {
        protected final JLabel myName = new JLabel();
        protected final JLabel myPath = new JLabel();
        protected boolean myHovered;
        protected JPanel myCloseThisItem;
        private final UniqueNameBuilder<ReopenProjectAction> myShortener;

        protected RecentProjectItemRenderer(UniqueNameBuilder<ReopenProjectAction> pathShortener) {
            super(new VerticalFlowLayout());
            this.myCloseThisItem = RecentProjectPanel.this.myCloseButtonForEditor;
            this.myShortener = pathShortener;
            this.myPath.setFont(Fonts.label(SystemInfo.isMac ? 10.0F : 11.0F));
            this.setFocusable(true);
            this.layoutComponents();
        }

        protected void layoutComponents() {
            this.add(this.myName);
            this.add(this.myPath);
        }

        protected Color getListBackground(boolean isSelected, boolean hasFocus) {
            return UIUtil.getListBackground(isSelected);
        }

        protected Color getListForeground(boolean isSelected, boolean hasFocus) {
            return UIUtil.getListForeground(isSelected);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            this.myHovered = RecentProjectPanel.this.myHoverIndex == index;
            Color fore = this.getListForeground(isSelected, list.hasFocus());
            Color back = this.getListBackground(isSelected, list.hasFocus());
            this.myName.setForeground(fore);
            this.myPath.setForeground(isSelected ? fore : UIUtil.getInactiveTextColor());
            this.setBackground(back);
            if (value instanceof ReopenProjectAction) {
                ReopenProjectAction item = (ReopenProjectAction)value;
                //FIXME: debug 修改名称
                String path = item.getProjectPath();
                if (path.charAt(path.length() - 1) == '/') {
                    path = path.substring(0, path.length() - 1);
                }
                int idx = path.lastIndexOf('/', path.lastIndexOf('/') - 1);
                String showName = path.substring(idx);
                this.myName.setText(showName);
//                this.myName.setText(item.getTemplatePresentation().getText());
                this.myPath.setText(this.getTitle2Text(item, this.myPath, JBUI.scale(40)));
            } else if (value instanceof ProjectGroupActionGroup) {
                ProjectGroupActionGroup group = (ProjectGroupActionGroup)value;
                this.myName.setText(group.getGroup().getName());
                this.myPath.setText("");
            }

            AccessibleContextUtil.setCombinedName(this, this.myName, " - ", this.myPath);
            AccessibleContextUtil.setCombinedDescription(this, this.myName, " - ", this.myPath);
            return this;
        }

        protected String getTitle2Text(ReopenProjectAction action, JComponent pathLabel, int leftOffset) {
            String fullText = action.getProjectPath();
            if (fullText != null && fullText.length() != 0) {
                fullText = FileUtil.getLocationRelativeToUserHome(PathUtil.toSystemDependentName(fullText), false);

                try {
                    FontMetrics fm = pathLabel.getFontMetrics(pathLabel.getFont());
                    int maxWidth = RecentProjectPanel.this.getWidth() - leftOffset;
                    if (maxWidth > 0 && fm.stringWidth(fullText) > maxWidth) {
                        int left = 1;
                        int right = 1;
                        int center = fullText.length() / 2;

                        String s;
                        for(s = fullText.substring(0, center - left) + "..." + fullText.substring(center + right); fm.stringWidth(s) > maxWidth; s = fullText.substring(0, center - left) + "..." + fullText.substring(center + right)) {
                            if (left == right) {
                                ++left;
                            } else {
                                ++right;
                            }

                            if (center - left < 0 || center + right >= fullText.length()) {
                                return "";
                            }
                        }

                        return s;
                    }
                } catch (Exception var11) {
                    RecentProjectPanel.LOG.error("Path label font: " + pathLabel.getFont());
                    RecentProjectPanel.LOG.error("Panel width: " + RecentProjectPanel.this.getWidth());
                    RecentProjectPanel.LOG.error(var11);
                }

                return fullText;
            } else {
                return " ";
            }
        }

        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.min(size.width, JBUI.scale(245)), size.height);
        }

        @NotNull
        public Dimension getSize() {
            Dimension var10000 = this.getPreferredSize();
            if (var10000 == null) {
//                $$$reportNull$$$0(0);
            }

            return var10000;
        }
    }

    private class MyList extends JBList<AnAction> {
        private final Dimension mySize;
        private Point myMousePoint;

        private MyList(RecentProjectPanel var1, @NotNull Dimension size, AnAction[] listData) {
            super(listData);
            if (listData == null) {
//                $$$reportNull$$$0(0);
            }

//            this.this$0 = var1;
            this.mySize = size;
            this.setEmptyText("  No Project Open Yet  ");
            this.setSelectionMode(2);
            this.getAccessibleContext().setAccessibleName("Recent Projects");
            RecentProjectPanel.MyList.MouseHandler handler = new RecentProjectPanel.MyList.MouseHandler();
            this.addMouseListener(handler);
            this.addMouseMotionListener(handler);
        }

        public Rectangle getCloseIconRect(int index) {
            Rectangle bounds = this.getCellBounds(index, index);
            Icon icon = Project.Remove;
            return new Rectangle(bounds.width - icon.getIconWidth() - 10, bounds.y + 10, icon.getIconWidth(), icon.getIconHeight());
        }

        public void paint(Graphics g) {
            super.paint(g);
            if (this.myMousePoint != null) {
                int index = this.locationToIndex(this.myMousePoint);
                if (index != -1) {
                    Rectangle iconRect = this.getCloseIconRect(index);
                    Icon icon = iconRect.contains(this.myMousePoint) ? Project.Remove_hover : Project.Remove;
                    icon.paintIcon(this, g, iconRect.x, iconRect.y);
                }
            }

        }

/*
        public String getToolTipText(MouseEvent event) {
            int i = this.locationToIndex(event.getPoint());
            if (i != -1) {
                Object elem = this.getModel().getElementAt(i);
                if (elem instanceof ReopenProjectAction) {
                    String path = ((ReopenProjectAction)elem).getProjectPath();
                    boolean valid = this.isPathValid(path);
                    if (!valid || this.this$0.projectsWithLongPathes.contains(elem)) {
                        String suffix = valid ? "" : " (unavailable)";
                        return PathUtil.toSystemDependentName(path) + suffix;
                    }
                }
            }

            return super.getToolTipText(event);
        }
*/

        public Dimension getPreferredScrollableViewportSize() {
            return this.mySize == null ? super.getPreferredScrollableViewportSize() : this.mySize;
        }

        class MouseHandler extends MouseAdapter {
            MouseHandler() {
            }

            public void mouseEntered(MouseEvent e) {
                MyList.this.myMousePoint = e.getPoint();
            }

            public void mouseExited(MouseEvent e) {
                MyList.this.myMousePoint = null;
            }

            public void mouseMoved(MouseEvent e) {
                MyList.this.myMousePoint = e.getPoint();
            }

            public void mouseReleased(MouseEvent e) {
                Point point = e.getPoint();
                RecentProjectPanel.MyList list = MyList.this;
                int index = list.locationToIndex(point);
                if (index != -1 && MyList.this.getCloseIconRect(index).contains(point)) {
                    e.consume();
                    Object element = MyList.this.getModel().getElementAt(index);
                    RecentProjectPanel.removeRecentProjectElement(element);
                    ListUtil.removeSelectedItems(MyList.this);
                }

            }
        }
    }
}
