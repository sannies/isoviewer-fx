package com.googlecode.mp4parser.mp4inspector;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.util.Path;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.util.Callback;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 *
 */
public class BoxPane extends TitledPane {
    static Properties names = new Properties();
    private static final Collection<String> skipList = Arrays.asList(
            "class",
            "boxes",
            "deadBytes",
            "type",
            "header",
            "isoFile",
            "parent",
            "content"
    );


    static {
        try {
            names.load(BoxPane.class.getResourceAsStream("/names.properties"));
        } catch (IOException e) {
            // i dont care
            throw new RuntimeException(e);
        }
    }


    Box box;

    public BoxPane(final Box box) {
        this.box = box;
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(box.getClass());
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        TableView<PropertyDescriptor> tableView = new TableView<PropertyDescriptor>();

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String name = propertyDescriptor.getName();
            if (!skipList.contains(name) &&
                    propertyDescriptor.getReadMethod() != null &&
                    !AbstractBox.class.isAssignableFrom(propertyDescriptor.getReadMethod().getReturnType())) {
                tableView.getItems().addAll(propertyDescriptor);
            }
        }
        TableColumn<PropertyDescriptor, String> propertyTableColumn = new TableColumn<PropertyDescriptor, String>("Property");
        TableColumn<PropertyDescriptor, Node> valueTableColumn = new TableColumn<PropertyDescriptor, Node>("Value");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        propertyTableColumn.setPrefWidth(10);
        propertyTableColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<PropertyDescriptor, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(final TableColumn.CellDataFeatures<PropertyDescriptor, String> propertyDescriptorStringCellDataFeatures) {
                return new StringBinding() {
                    @Override
                    protected String computeValue() {
                        return propertyDescriptorStringCellDataFeatures.getValue().getName();
                    }
                };
            }
        });
        valueTableColumn.setPrefWidth(20);
        valueTableColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<PropertyDescriptor, Node>, ObservableValue<Node>>() {
            @Override
            public ObservableValue<Node> call(final TableColumn.CellDataFeatures<PropertyDescriptor, Node> propertyDescriptorStringCellDataFeatures) {
                return new ObjectBinding<Node>() {
                    @Override
                    protected Node computeValue() {
                        try {
                            Object o = propertyDescriptorStringCellDataFeatures.getValue().getReadMethod().invoke(box);
                            if (o instanceof List) {
                                ListView<Object> lv = new ListView<Object>(new ObservableListWrapper<Object>((List<Object>) o));
                                lv.setMinHeight(20);
                                lv.setPrefHeight(((List) o).size() * 15 + 20);
                                lv.setMaxHeight(200);
                                TitledPane tp = new TitledPane("List contents", lv);
                                tp.setExpanded(false);
                                return tp;
                            } else if (o != null && o.getClass().isArray()) {
                                int length = Array.getLength(o);
                                List<Object> values = new LinkedList<Object>();
                                for (int i = 0; i < length; i++) {
                                    Object value = Array.get(o, i);
                                    if (value instanceof Box) {
                                        values.add(Path.createPath((Box) value));
                                    } else {
                                        values.add(value);
                                    }
                                }

                                ListView<Object> lv = new ListView<Object>(new ObservableListWrapper<Object>(values));
                                lv.setMinHeight(20);
                                lv.setPrefHeight(length * 15 + 20);
                                lv.setMaxHeight(200);
                                TitledPane tp = new TitledPane("List contents", lv);
                                tp.setExpanded(false);
                                return tp;
                            } else {
                                TextField t = new TextField(o != null ? o.toString() : "null");
                                t.setEditable(false);
                                return t;
                            }
                        } catch (IllegalAccessException e) {
                            return new Text(e.getLocalizedMessage());
                        } catch (InvocationTargetException e) {
                            return new Text(e.getLocalizedMessage());
                        }
                    }
                };
            }
        });

        tableView.getColumns().addAll(propertyTableColumn, valueTableColumn);
        setContent(tableView);
        setCollapsible(false);
        if (box instanceof IsoFile) {
            setText("ISO File");
        } else {
            setText(names.getProperty(box.getType(), "Unknown Box"));
        }
    }
}
