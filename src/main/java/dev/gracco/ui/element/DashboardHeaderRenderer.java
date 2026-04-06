package dev.gracco.ui.element;

import dev.gracco.ui.Theme;
import dev.gracco.ui.Theme.FontType;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public class DashboardHeaderRenderer implements TableCellRenderer {
    private final TableCellRenderer delegate;

    public DashboardHeaderRenderer(TableCellRenderer delegate) {
        this.delegate = delegate;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
    ) {
        Component component = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (component instanceof JLabel label) {
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setOpaque(true);
            label.setBackground(Theme.BACKGROUND_GREEN);
            label.setForeground(Theme.BLACK);
            label.setFont(Theme.getFont(FontType.MEDIUM, 14f));
            label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.SECONDARY));
            label.setIconTextGap(8);

            if (label.getIcon() == null) {
                Icon ascendingIcon = UIManager.getIcon("Table.ascendingSortIcon");
                label.setDisabledIcon(ascendingIcon);
            }
        }

        return component;
    }
}