package app.settingswindow;

import app.ControllerBase;
import app.Window;
import app.mainwindow.MainWindowController;
import app.util.FontHelper;
import app.util.FontConfig;
import javafx.animation.RotateTransition;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class SettingsWindowController extends ControllerBase {
    @FXML
    private VBox fontOptionsVBox;
    @FXML
    private ImageView fontArrow;
    @FXML
    private HBox appearanceHBox, fontHBox;
    @FXML
    private VBox editorFontOptionsVBox, consoleFontOptionsVBox;
    @FXML
    private HBox editorHBox, consoleHBox;
    @FXML
    private ComboBox<String> editorFontFamilyComboBox, consoleFontFamilyComboBox;
    @FXML
    private Spinner<Double> editorFontSizeSpinner, consoleFontSizeSpinner;
    @FXML
    private TextArea editorFontPreviewTextArea, consoleFontPreviewTextArea;

    private boolean fontOptionsVisible = false;
    private MainWindowController mainWindowController;

    private double tempEditorFontSize, tempConsoleFontSize;
    private String tempEditorFontFamily = FontConfig.getEditorFontFamily();
    private String tempConsoleFontFamily = FontConfig.getConsoleFontFamily();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Window.getWindowAt(Window.SETTINGS_WINDOW).setController(this);
        mainWindowController = (MainWindowController) Window.getWindowAt(Window.MAIN_WINDOW).getController();
        setupFontOptions();
    }

    private void setupFontOptions() {
        setupFontFamilyComboBox(editorFontFamilyComboBox, FontConfig.EDITOR_FONT_FAMILIES, FontConfig.getEditorFontFamily(), editorFontPreviewTextArea);
        setupFontFamilyComboBox(consoleFontFamilyComboBox, FontConfig.CONSOLE_FONT_FAMILIES, FontConfig.getConsoleFontFamily(), consoleFontPreviewTextArea);
        setupFontSizeSpinner(editorFontSizeSpinner, FontConfig.EDITOR_MIN_FONT_SIZE, FontConfig.EDITOR_MAX_FONT_SIZE, FontConfig.getEditorFontSize(), editorFontPreviewTextArea);
        setupFontSizeSpinner(consoleFontSizeSpinner, FontConfig.CONSOLE_MIN_FONT_SIZE, FontConfig.CONSOLE_MAX_FONT_SIZE, FontConfig.getConsoleFontSize(), consoleFontPreviewTextArea);
        setupPreviewTextAreas();
    }

    private void setupFontFamilyComboBox(ComboBox<String> comboBox, String[] fontFamilies, String defaultFamily, TextArea previewArea) {
        comboBox.getItems().addAll(fontFamilies);
        comboBox.setValue(defaultFamily);
        comboBox.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            updateTempFontFamily(comboBox, newValue);
            updatePreviewAreaFontFamily(previewArea, newValue);
        });
    }

    private void updateTempFontFamily(ComboBox<String> comboBox, String newValue) {
        if (comboBox == editorFontFamilyComboBox) {
            tempEditorFontFamily = newValue;
        } else if (comboBox == consoleFontFamilyComboBox) {
            tempConsoleFontFamily = newValue;
        }
    }

    private void updatePreviewAreaFontFamily(TextArea previewArea, String fontFamily) {
        previewArea.getStyleClass().removeAll("calibri-font", "monospaced-font");
        previewArea.getStyleClass().add(fontFamily.equals(FontConfig.MONOSPACED_FONT) ? "monospaced-font" : "calibri-font");
    }

    private void setupFontSizeSpinner(Spinner<Double> spinner, double min, double max, double initial, TextArea previewArea) {
        SpinnerValueFactory<Double> factory = new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initial, FontConfig.FONT_STEP);
        spinner.setValueFactory(factory);
        spinner.valueProperty().addListener((ObservableValue<? extends Double> observable, Double oldValue, Double newValue) -> {
            previewArea.setStyle("-fx-font-size: " + newValue + "px;");
            updateTempFontSize(spinner, newValue);
        });
    }

    private void updateTempFontSize(Spinner<Double> spinner, double newValue) {
        if (spinner == editorFontSizeSpinner) {
            tempEditorFontSize = newValue;
        } else if (spinner == consoleFontSizeSpinner) {
            tempConsoleFontSize = newValue;
        }
    }

    private void setupPreviewTextAreas() {
        editorFontPreviewTextArea.setEditable(false);
        consoleFontPreviewTextArea.setEditable(false);
    }

    @FXML
    private void OKSettings() {
        applySettings();
        Window.hideWindow(Window.SETTINGS_WINDOW);
    }

    @FXML
    private void applySettings() {
        if (editorFontOptionsVBox.isVisible()) {
            mainWindowController.editorArea.setId(tempEditorFontFamily.toLowerCase());
            FontHelper.setFontSize((int) tempEditorFontSize, mainWindowController.editorArea);
        //    FontConfig.updateFontConfig(tempEditorFontFamily, tempEditorFontSize, true);
        } else if (consoleFontOptionsVBox.isVisible()) {
            mainWindowController.consoleTextFlow.setId(tempConsoleFontFamily.toLowerCase());
            System.out.println("Console font size: " + (int) tempConsoleFontSize);
            mainWindowController.consoleTextFlow.setStyle("-fx-font-size: " + (int) tempConsoleFontSize + "px;");
        }
    }

    @FXML
    private void cancelSettings() {
        Window.hideWindow(Window.SETTINGS_WINDOW);
    }

    @FXML
    private void toggleFontOptions() {
        fontOptionsVisible = !fontOptionsVisible;
        animateFontArrow();
        fontOptionsVBox.setVisible(fontOptionsVisible);
        fontOptionsVBox.setManaged(fontOptionsVisible);
        selectFontOptions();
    }

    private void animateFontArrow() {
        RotateTransition rotateTransition = new RotateTransition(Duration.millis(200), fontArrow);
        rotateTransition.setByAngle(fontOptionsVisible ? 90 : -90);
        rotateTransition.play();
        fontArrow.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("../resources/icons/arrow_right_icon.png"))));
    }

    @FXML
    private void selectAppearance() {
        setSelectedOption(appearanceHBox);
    }

    @FXML
    private void selectFontOptions() {
        setSelectedOption(fontHBox);
    }

    @FXML
    private void selectEditor() {
        setSelectedOption(editorHBox);
        showFontOptions(editorFontOptionsVBox, consoleFontOptionsVBox);
    }

    @FXML
    private void selectConsole() {
        setSelectedOption(consoleHBox);
        showFontOptions(consoleFontOptionsVBox, editorFontOptionsVBox);
    }

    private void setSelectedOption(HBox selectedBox) {
        clearSelection();
        selectedBox.getStyleClass().add("selected");
    }

    private void showFontOptions(VBox showBox, VBox hideBox) {
        showBox.setVisible(true);
        hideBox.setVisible(false);
    }

    private void clearSelection() {
        appearanceHBox.getStyleClass().remove("selected");
        consoleHBox.getStyleClass().remove("selected");
        editorHBox.getStyleClass().remove("selected");
        fontHBox.getStyleClass().remove("selected");
    }
}
