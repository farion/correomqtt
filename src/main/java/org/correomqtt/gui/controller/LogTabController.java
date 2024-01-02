package org.correomqtt.gui.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.correomqtt.business.eventbus.EventBus;
import org.correomqtt.business.eventbus.Subscribe;
import org.correomqtt.business.log.LogEvent;
import org.correomqtt.business.log.PopLogCache;
import org.correomqtt.business.log.SaveLogTask;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class LogTabController extends BaseControllerImpl {

    private static ResourceBundle resources;
    private final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LogTabController.class);

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'_'HH-mm-ss");

    @FXML
    public AnchorPane logViewAnchor;
    @FXML
    public CodeArea logTextArea;
    @FXML
    public ToggleButton followButton;
    @FXML
    public ComboBox<Level> logLevel;
    private Logger rootLogger;

    public LogTabController() {
        EventBus.register(this);
    }

    public static LoaderResult<LogTabController> load() {
        LoaderResult<LogTabController> result = load(LogTabController.class, "logView.fxml");
        resources = result.getResourceBundle();
        return result;
    }

    @FXML
    private void initialize() {
        logLevel.getItems().addAll(
                Level.ERROR,
                Level.WARN,
                Level.INFO,
                Level.DEBUG,
                Level.TRACE
        );
        rootLogger = (Logger) LoggerFactory.getLogger("org.correomqtt");

        logLevel.getSelectionModel().select(rootLogger.getLevel());

        followButton.setSelected(true);
        EventBus.fire(new PopLogCache());
    }

    @SuppressWarnings("unused")
    public void updateLog(@Subscribe LogEvent event) {
        String[] matches = event.logMsg().split("\u001B");
        String cssClass;
        for (String match : matches) {
            String str;
            if (match.startsWith("[36m")) {
                cssClass = "cyan";
                str = match.substring(4);
            } else if (match.startsWith("[34m")) {
                cssClass = "blue";
                str = match.substring(4);
            } else if (match.startsWith("[31m")) {
                cssClass = "orange";
                str = match.substring(4);
            } else if (match.startsWith("[33m")) {
                cssClass = "yellow";
                str = match.substring(4);
            } else if (match.startsWith("[35m")) {
                cssClass = "magenta";
                str = match.substring(4);
            } else if (match.startsWith("[39m")) {
                cssClass = "default";
                str = match.substring(4);
            } else if (match.startsWith("[1;31m")) {
                cssClass = "red";
                str = match.substring(6);
            } else if (match.startsWith("[0;39m")) {
                cssClass = "default";
                str = match.substring(6);
            } else {
                cssClass = "default";
                str = match;
            }
            logTextArea.append(str, cssClass);
        }
        followCaret();
    }


    public void cleanUp() {
        EventBus.unregister(this);
    }

    public void onTrashButtonClicked() {
        logTextArea.clear();
    }

    public void onToggleButtonClicked() {
        followCaret();
    }

    private void followCaret() {
        if (followButton.isSelected()) {
            logTextArea.requestFollowCaret();
        }
    }

    @FXML
    public void onLogLevelChange() {
        logTextArea.append("Log Level changed to " + logLevel.getValue() + "\n", "blue");
        rootLogger.setLevel(logLevel.getValue());
    }

    @FXML
    public void onTest( ) {
        LOGGER.error("ERRORTEST");
        LOGGER.warn("WARNTEST");
        LOGGER.info("INFOTEST");
        LOGGER.debug("DEBUGTEST");
        LOGGER.trace("TRACETEST");
    }

    @FXML
    public void onSave( ) {

        Stage stage = (Stage) logViewAnchor.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(resources.getString("exportUtilsTitle"));
        fileChooser.setInitialFileName("correo_" + LocalDateTime.now().atOffset(ZoneOffset.UTC).format(dateTimeFormatter) + ".log");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(resources.getString("exportUtilsDescription"), "*.log");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(stage);

        new SaveLogTask(file, logTextArea.getText())
                .onSuccess(this::onSaveSucceeded)
                .onError(this::onSaveFailed)
                .run();

    }

    private void onSaveFailed(Throwable throwable) {
       //TODO
    }

    private void onSaveSucceeded(Void unused) {
        //TODO
    }
}
