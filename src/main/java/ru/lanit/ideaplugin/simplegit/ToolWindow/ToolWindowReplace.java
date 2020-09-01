package ru.lanit.ideaplugin.simplegit.ToolWindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.actions.ReplaceStepsAction;

import javax.swing.*;

public class ToolWindowReplace implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final JFXPanel fxPanel = new JFXPanel();
        JComponent component = toolWindow.getComponent();

        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            Group root = new Group();
            Scene scene = new Scene(root);
            scene.setFill(Color.rgb(105, 105, 105));
            //Заголовок
            Text heading = new Text("Настройки нумерации");
            heading.setFill(Color.rgb(255, 245, 238));
            heading.setX(50);
            heading.setY(50);
            heading.setFont(new Font(25));
            // Редактиование отступа шага ScrollBar
            Text text = new Text("Отступ шага");
            text.setFill(Color.rgb(255, 245, 238));
            text.setX(10);
            text.setY(100);
            text.setFont(new Font(20));

            ScrollBar scrollableY = new ScrollBar();
            scrollableY.setMin(0);
            scrollableY.setMax(9);
            scrollableY.setValue(4);
            scrollableY.setOrientation(Orientation.HORIZONTAL);
            scrollableY.setTranslateY(88);
            scrollableY.setTranslateX(150);
            scrollableY.setPrefWidth(180);
            scrollableY.setStyle("-fx-background-color: #000000;" +
                    "-fx-background-radius: 5em;" +
                    "-fx-border-color: transparent;");

            scrollableY.valueProperty().addListener(event -> {
                if (scrollableY.getValue() < 1) {
                    ReplaceStepsAction.indentation = "";
                } else {
                    ReplaceStepsAction.indentation = String.format("%" + ((int) scrollableY.getValue()) + "s", " ");
                }
            });
            Tooltip tooltip = new Tooltip("Количество пробелов");
            scrollableY.setTooltip(tooltip);

            //Чекбокс Ставить шаг в пустых строках
            CheckBox isSetStepVoid = new CheckBox("Ставить шаг в пустых строках");
            isSetStepVoid.selectedProperty().addListener(event -> {
                ReplaceStepsAction.isSetStepVoid = isSetStepVoid.isSelected();
                System.out.println(isSetStepVoid.isSelected());
            });
            isSetStepVoid.setTranslateX(10);
            isSetStepVoid.setTranslateY(125);
            isSetStepVoid.setFont(new Font(15));
            isSetStepVoid.setTextFill(Color.rgb(255, 245, 238));



            //Чекбокс Ставить пробел между шагами
            CheckBox checkBox = new CheckBox("Ставить пустую строку между шагами");
            checkBox.selectedProperty().addListener(event -> {
                ReplaceStepsAction.isStepSeparator = checkBox.isSelected();
                System.out.println(checkBox.isSelected());
            });
            checkBox.setTranslateX(10);
            checkBox.setTranslateY(170);
            checkBox.setFont(new Font(15));
            checkBox.setTextFill(Color.rgb(255, 245, 238));

            root.getChildren().add(heading);
            root.getChildren().add(scrollableY);
            root.getChildren().add(text);
            root.getChildren().add(checkBox);
            root.getChildren().add(isSetStepVoid);

            fxPanel.setScene(scene);

        });

        component.getParent().add(fxPanel);
    }
}
