package org.correomqtt.gui.cell;

import javafx.application.Platform;
import org.correomqtt.business.dispatcher.ConnectionLifecycleDispatcher;
import org.correomqtt.business.dispatcher.ConnectionLifecycleObserver;
import org.correomqtt.business.model.CorreoMqttVersion;
import org.correomqtt.business.model.Lwt;
import org.correomqtt.business.model.Proxy;
import org.correomqtt.business.model.TlsSsl;
import org.correomqtt.business.mqtt.CorreoMqttClient;
import org.correomqtt.business.provider.SettingsProvider;
import org.correomqtt.business.utils.ConnectionHolder;
import org.correomqtt.gui.model.ConnectionPropertiesDTO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import org.correomqtt.gui.transformer.ConnectionTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionCell extends ListCell<ConnectionPropertiesDTO> implements ConnectionLifecycleObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionCell.class);
    private final ListView<ConnectionPropertiesDTO> listView;

    @FXML
    private Pane mainNode;
    @FXML
    private Label nameLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label credentialsTag;
    @FXML
    private Label sslTag;
    @FXML
    private Label proxyTag;
    @FXML
    private Label lwtTag;
    @FXML
    private Label mqtt3Tag;
    @FXML
    private Label mqtt5Tag;
    @FXML
    private ResourceBundle resources;
    @FXML
    private Label statusLabel;

    private FXMLLoader loader;

    private ConnectionPropertiesDTO connectionDTO;

    @FXML
    public void initialize() {
        ConnectionLifecycleDispatcher.getInstance().addObserver(this);

    }

    public ConnectionCell(ListView<ConnectionPropertiesDTO> listView) {
        this.listView = listView;
    }

    @Override
    protected void updateItem(ConnectionPropertiesDTO connectionDTO, boolean empty) {
        this.connectionDTO = connectionDTO;
        super.updateItem(connectionDTO, empty);

        if (empty || connectionDTO == null) {
            setText(null);
            setGraphic(null);
        } else {

            if (loader == null) {
                try {
                    loader = new FXMLLoader(SubscriptionViewCell.class.getResource("connectionView.fxml"),
                            ResourceBundle.getBundle("org.correomqtt.i18n", SettingsProvider.getInstance().getSettings().getCurrentLocale()));
                    loader.setController(this);
                    loader.load();

                } catch (Exception e) {
                    LOGGER.error("Exception rendering connection:", e);
                    setText(resources.getString("commonRowCreationError"));
                    setGraphic(null);
                    return;
                }

            }
            mainNode.getStyleClass().add(SettingsProvider.getInstance().getIconModeCssClass());
            if(listView != null) {
                mainNode.prefWidthProperty().bind(listView.widthProperty().subtract(20));
            }
            setConnection(connectionDTO);
            setText(null);
            setGraphic(mainNode);
        }
    }

    private void setConnection(ConnectionPropertiesDTO connectionDTO) {

        //TODO parent css class only
        if (connectionDTO.isDirty()) {
            nameLabel.setText(connectionDTO.getName() + " *");
            nameLabel.getStyleClass().removeAll("dirty");
            nameLabel.getStyleClass().add("dirty");

            descriptionLabel.getStyleClass().removeAll("dirty");
            descriptionLabel.getStyleClass().add("dirty");

            mqtt3Tag.getStyleClass().removeAll("inactive");
            mqtt3Tag.getStyleClass().add("inactive");

            mqtt5Tag.getStyleClass().removeAll("inactive");
            mqtt5Tag.getStyleClass().add("inactive");

            credentialsTag.getStyleClass().removeAll("inactive");
            credentialsTag.getStyleClass().add("inactive");

            sslTag.getStyleClass().removeAll("inactive");
            sslTag.getStyleClass().add("inactive");

            proxyTag.getStyleClass().removeAll("inactive");
            proxyTag.getStyleClass().add("inactive");

            lwtTag.getStyleClass().removeAll("inactive");
            lwtTag.getStyleClass().add("inactive");
        } else {
            nameLabel.setText(connectionDTO.getName());
            nameLabel.getStyleClass().removeAll("dirty");
            descriptionLabel.getStyleClass().removeAll("dirty");
            mqtt3Tag.getStyleClass().removeAll("inactive");
            mqtt5Tag.getStyleClass().removeAll("inactive");
            credentialsTag.getStyleClass().removeAll("inactive");
            sslTag.getStyleClass().removeAll("inactive");
            proxyTag.getStyleClass().removeAll("inactive");
            lwtTag.getStyleClass().removeAll("inactive");
        }

        descriptionLabel.setText(connectionDTO.getHostAndPort());

        if (ConnectionHolder.getInstance().isConnectionUnused(ConnectionTransformer.propsToDto(connectionDTO))) {
            setGray();
        } else {
            CorreoMqttClient client = ConnectionHolder.getInstance().getConnection(getConnectionId()).getClient();
            if (client == null) {
                setGray();
            } else {
                switch (client.getState()) {
                    case CONNECTED:
                        setGreen();
                        break;
                    case RECONNECTING:
                    case CONNECTING:
                        setYellow();
                        break;
                    case FAILED:
                        setRed();
                        break;
                    case DISCONNECTED:
                    case UNKOWN:
                        setGray();
                        break;
                }
            }
        }

        boolean mqtt3 = connectionDTO.getMqttVersionProperty().getValue().equals(CorreoMqttVersion.MQTT_3_1_1);
        mqtt3Tag.setVisible(mqtt3);
        mqtt3Tag.setManaged(mqtt3);

        boolean mqtt5 = connectionDTO.getMqttVersionProperty().getValue().equals(CorreoMqttVersion.MQTT_5_0);
        mqtt5Tag.setVisible(mqtt5);
        mqtt5Tag.setManaged(mqtt5);

        boolean credentials = connectionDTO.getUsername() != null && !connectionDTO.getUsername().isEmpty()
                && connectionDTO.getPassword() != null && !connectionDTO.getPassword().isEmpty();
        credentialsTag.setVisible(credentials);
        credentialsTag.setManaged(credentials);

        boolean ssl = connectionDTO.getSslProperty().getValue().equals(TlsSsl.KEYSTORE);
        sslTag.setVisible(ssl);
        sslTag.setManaged(ssl);

        boolean proxy = connectionDTO.getProxyProperty().getValue().equals(Proxy.SSH);
        proxyTag.setVisible(proxy);
        proxyTag.setManaged(proxy);

        boolean lwt = connectionDTO.getLwtProperty().getValue().equals(Lwt.ON);
        lwtTag.setVisible(lwt);
        lwtTag.setManaged(lwt);
    }

    private void setGreen() {
        Platform.runLater(() -> {
            statusLabel.getStyleClass().remove("redStatus");
            statusLabel.getStyleClass().remove("grayStatus");
            statusLabel.getStyleClass().remove("yellowStatus");
            statusLabel.getStyleClass().add("greenStatus");
        });
    }

    private void setRed() {
        Platform.runLater(() -> {
            statusLabel.getStyleClass().remove("grayStatus");
            statusLabel.getStyleClass().remove("greenStatus");
            statusLabel.getStyleClass().remove("yellowStatus");
            statusLabel.getStyleClass().add("redStatus");
        });
    }

    private void setGray() {
        Platform.runLater(() -> {
            statusLabel.getStyleClass().remove("redStatus");
            statusLabel.getStyleClass().remove("greenStatus");
            statusLabel.getStyleClass().remove("yellowStatus");
            statusLabel.getStyleClass().add("grayStatus");
        });
    }

    private void setYellow() {
        Platform.runLater(() -> {
            statusLabel.getStyleClass().remove("redStatus");
            statusLabel.getStyleClass().remove("greenStatus");
            statusLabel.getStyleClass().remove("grayStatus");
            statusLabel.getStyleClass().add("yellowStatus");
        });
    }

    @Override
    public void onDisconnectFromConnectionDeleted(String connectionId) {
        if (connectionId.equals(getConnectionId())) {
            setGray();
        }
    }

    @Override
    public void onConnect() {
        setGreen();
    }

    @Override
    public void onConnectRunning() {
        setYellow();
    }

    @Override
    public void onConnectionFailed(Throwable message) {
        setRed();
    }

    @Override
    public void onConnectionLost() {
        setRed();
    }

    @Override
    public void onDisconnect() {
        setGray();
    }

    @Override
    public void onDisconnectFailed(Throwable exception) {
        setRed();
    }

    @Override
    public void onDisconnectRunning() {
        setYellow();
    }

    @Override
    public void onConnectionReconnected() {
        setGreen();
    }

    @Override
    public void onReconnectFailed(AtomicInteger triedReconnects, int maxReconnects) {
        setRed();
    }

    @Override
    public String getConnectionId() {
        if (connectionDTO != null) {
            return connectionDTO.getId();
        }
        return "";
    }
}
