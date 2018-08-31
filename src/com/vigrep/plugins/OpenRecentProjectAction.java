package com.vigrep.plugins;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogBuilder;

/**
 * 因为android studio 的open recent project 时，名称总是显示项目的根目录名，路径长的时候，显示不全，面板又不可调整大小
 * 故开发该插件，自由定制open recent project显示内容
 *
 * Created by RS on 2018/8/31.
 */
public class OpenRecentProjectAction extends AnAction {

    private DialogBuilder dialogBuilder;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        dialogBuilder = new DialogBuilder().centerPanel(new RecentProjectPanel(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (dialogBuilder != null)
                    dialogBuilder.getDialogWrapper().close(1);
            }
        })).title("最近工程").resizable(true);
        dialogBuilder.removeAllActions();
        dialogBuilder.addCancelAction();
        dialogBuilder.showNotModal();

//        Project project = e.getData(PlatformDataKeys.PROJECT);
//        File file = new File("/Users/xwj-imac2/Library/Preferences/AndroidStudio3.1/options/recentProjects.xml");
//        AnAction[] anActionList = RecentProjectsManager.getInstance().getRecentProjectsActions(true);

/*        RecentProjectsGroup recentProjectsGroup = new RecentProjectsGroup();
        AnAction[] anActionList = recentProjectsGroup.getChildren(e);
        List<AnAction> actionList = Arrays.asList(anActionList);
        JBPanel jPanel = new JBPanel();
        JBList<AnAction> jList = new JBList<>(new MyListModel(actionList));
        jList.setCellRenderer(new ListCellRenderer<AnAction>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends AnAction> list, AnAction action, int index, boolean isSelected, boolean cellHasFocus) {
                JPanel cellPanel = new JPanel();
                if (action instanceof ReopenProjectAction) {
                    String path = ((ReopenProjectAction) action).getProjectPath();
                    if (path.charAt(path.length() - 1) == '/') {
                        path = path.substring(0, path.length() - 1);
                    }
                    int idx = path.lastIndexOf('/', path.lastIndexOf('/') - 1);
                    String showName = path.substring(idx);

                    cellPanel.setBounds(0,0, 800,60);
                    cellPanel.add(new JLabel(showName + "  ："));
                    cellPanel.add(new JLabel(path));
                } else {
                    cellPanel.add(new JLabel("无工程"));
                }
                return cellPanel;
            }
        });
        jList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                String path = ((ReopenProjectAction) jList.getSelectedValue()).getProjectPath();
                RecentProjectsManagerImpl.getInstanceEx().doOpenProject(path, null , true);
                if (dialogBuilder != null)
                    dialogBuilder.getDialogWrapper().close(1);
            }
        });
        jList.setExpandableItemsEnabled(true);
        jList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        jList.setUI(new ListUI() {
            @Override
            public int locationToIndex(JList list, Point location) {
                return 0;
            }

            @Override
            public Point indexToLocation(JList list, int index) {
                return null;
            }

            @Override
            public Rectangle getCellBounds(JList list, int index1, int index2) {
                return null;
            }
        });
        jPanel.add(jList);

        dialogBuilder = new DialogBuilder().centerPanel(jPanel).title("最近工程").resizable(true);*/
    }
}
