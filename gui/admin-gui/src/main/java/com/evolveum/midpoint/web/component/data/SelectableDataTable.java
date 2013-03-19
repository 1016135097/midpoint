package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxPanel;
import com.evolveum.midpoint.web.component.util.Selectable;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.List;

public class SelectableDataTable<T> extends DataTable<T, String> {

    public SelectableDataTable(String id, List<IColumn<T, String>> columns, IDataProvider<T> dataProvider, int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
    }

    @Override
    protected Item<T> newRowItem(String id, int index, final IModel<T> model) {
        final Item<T> rowItem = new SelectableRowItem<T>(id, index, model);

        rowItem.setOutputMarkupId(true);

        rowItem.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                T object = rowItem.getModel().getObject();
                if (!(object instanceof Selectable)) {
                    return "";
                }

                Selectable selectable = (Selectable) object;
                return selectable.isSelected() ? "selectedRow" : "";
            }
        }));

        rowItem.add(new AjaxEventBehavior("onclick") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                T object = rowItem.getModel().getObject();
                if (!(object instanceof Selectable)) {
                    return;
                }

                Selectable selectable = (Selectable) object;
                selectable.setSelected(!selectable.isSelected());

                //update table row
                target.add(rowItem);
                //update checkbox header column, if we found some
                CheckBoxPanel headerCheck = CheckBoxHeaderColumn.findCheckBoxColumnHeader(SelectableDataTable.this);
                if (headerCheck == null) {
                    return;
                }

                headerCheck.getPanelComponent().setModelObject(
                        CheckBoxHeaderColumn.shoulBeHeaderSelected(SelectableDataTable.this));
                target.add(headerCheck);
            }
        });

        return rowItem;
    }

    public static class SelectableRowItem<T> extends Item<T> {

        public SelectableRowItem(String id, int index, IModel<T> model) {
            super(id, index, model);
        }
    }
}
