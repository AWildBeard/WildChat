/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.sun.javafx.font.freetype.HBGlyphLayout;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.util.ArrayList;

import static logUtils.Logger.*;

public class WildChat extends Application
{
    private static String[] launchArgs;

    private Stage primaryStage = null;

    private Scene root;

    private Thread baseConnectionThread = null;

    private UISettings uiSettings = new UISettings();

    private TwitchConnect socketRunner = null;

    static Client client = null;

    static Session session = new Session();

    private SplitPane mainContentHolder = new SplitPane();

    private GridPane mainContent = new GridPane();

    private HBox menuBar = new HBox();

    private Button connectButton = new Button("Connect"),
            uiSettingsButton = new Button("Customize UI"),
            disconnectButton = new Button("Disconnect");

    private static ScrollPane messagePane = new ScrollPane(),
            userListPane = new ScrollPane();

    static UserList userList = new UserList();

    private static VBox messageHolder = new VBox();

    private TextField messageField = new TextField();

    private ColumnConstraints column1Constraints = new ColumnConstraints(),
            column2Constraints = new ColumnConstraints();

    private RowConstraints row1Constraints = new RowConstraints(),
            row2Constraints = new RowConstraints(),
            row3Constraints = new RowConstraints();

    private final String filePrefix = ".WildChat/",
                         credentials = "credentials.dat",
                         uiSettingsFileName = "uisettings.dat",
                         dotDirLocation = (System.getProperty("os.name").contains("Windows")) ?
                             BasicIO.getEnvVars("APPDATA") + "/" : BasicIO.getEnvVars("HOME") + "/",
                         credentialFile = dotDirLocation + filePrefix + credentials,
                         uiSettingsFile = dotDirLocation + filePrefix + uiSettingsFileName;

    // 1d1f26
    static String textFill, backgroundColor, highlightColor, uiAccentColor, highlightTextColor;

    static double messageFontSize, uiFont;

    static volatile boolean connected = false,
        connectionMessageReceived = false,
        connectedToChannel = false,
        credentialsAvailable = false,
        hasUserState = false;

    public WildChat()
    {
        boolean canAccessCredentials,
                canAccessUiSettings,
                debug = true;

        if (launchArgs.length > 0 && launchArgs[0].equals("--no-debug"))
            debug = false;

        setShouldLog(debug);

        log("Testing read/write on " + credentialFile);
        if (! FileUtil.canReadWrite(credentialFile))
        {
            log("File " + credentialFile + " either does not exist or can't be read/wrote to");
            log("Attempting to create necessary dirs and file for " + credentialFile);
            canAccessCredentials = FileUtil.createFileWithDirs(credentialFile);
        }
        else
            canAccessCredentials = true;

        log((canAccessCredentials) ? "Can read/write " + credentialFile : "Can't read/write " + credentialFile);

        if (credentialsAvailable = FileUtil.hasData(credentialFile))
        {
            log("Credential file has data");
            try
            {
                ObjectInputStream is = new ObjectInputStream(new FileInputStream(new File(credentialFile)));
                client = (Client) is.readObject();
                is.close();
            }
            catch (IOException | ClassNotFoundException e )
            {
                log("Unrecoverable credential read operation");
                return;
            }
        }
        log((credentialsAvailable) ? "Credentials read in" : "No credential data found");

        log("Testing read/write on " + uiSettingsFile);
        if (! FileUtil.canReadWrite(uiSettingsFile))
        {
            log("File " + uiSettingsFile + " either does not exist or can't be read/wrote to");
            log("Attempting to create necessary dirs and file for " + uiSettingsFile);
            canAccessUiSettings = FileUtil.createFileWithDirs(uiSettingsFile);
        }
        else
            canAccessUiSettings = true;

        log((canAccessUiSettings) ? "Can read/write " + uiSettingsFile : "Can't read/write " + uiSettingsFile);

        if (FileUtil.hasData(uiSettingsFile))
        {
            log("UISettings file has data");
            try
            {
                ObjectInputStream is = new ObjectInputStream(new FileInputStream(new File(uiSettingsFile)));
                uiSettings = (UISettings) is.readObject();
                is.close();
            }
            catch (IOException | ClassNotFoundException e)
            {
                log("Unrecoverable UISettings read operation");
            }
        }

        log("Setting global UI vars");
        textFill = uiSettings.getTextFill();
        backgroundColor = uiSettings.getBackgroundColor();
        highlightColor = uiSettings.getHighlightColor();
        highlightTextColor = uiSettings.getHighlightTextColor();
        uiAccentColor = uiSettings.getUIAccentColor();
        messageFontSize = uiSettings.getMessageFontSize();
        uiFont = uiSettings.getUiFont();
    }

    @Override
    public void stop()
    {
        connected = false;
        log("ShutDown");
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage)
    {
        log("Stage start");
        this.primaryStage = primaryStage;

        log((! credentialsAvailable) ? "No credentials available, showing input fields" : "");
        if (! credentialsAvailable)
            askForCredentials();

        if (client == null)
        {
            log("Client not initialized!");
            System.exit(1);
        }

        log("Setting up networking");
        socketRunner = new TwitchConnect(client);

        log("Setting scene");
        root = new Scene(mainContent, 650, 400);

        log("initializing UI");
        initUI();

        this.primaryStage.setScene(root);
        this.primaryStage.setTitle("WildChat");

        // CTRL+Q exit application
        this.primaryStage.getScene().getAccelerators().put(KeyCombination.keyCombination("CTRL+Q"),() -> stop());

        log("Showing window");
        this.primaryStage.show();

        // call post shown ui props
        initPostUI();

        log("Starting networking");
        baseConnectionThread = new Thread(socketRunner);
        baseConnectionThread.start();
        log("Networking started");
        displayMessage("> Connecting to twitch.tv...");
    }

    private void initUI()
    {
        setVisibleProperties();
        setInteractions();
        addNodesToParents();
    }

    private void initPostUI()
    {
        Node divider = mainContentHolder.lookup(".split-pane-divider");
        Node scrollBar = messagePane.lookup(".scroll-bar");

        if (divider != null)
            divider.setStyle("-fx-background-color: " + uiAccentColor + ";");

        if (scrollBar != null)
        {
            Node thumb = scrollBar.lookup(".thumb");
            Node track = scrollBar.lookup(".track");
            Node incrementButton = scrollBar.lookup(".increment");
            Node decrementButton = scrollBar.lookup(".decrement");

            if (thumb != null)
                thumb.setStyle("-fx-background-color: " + uiAccentColor + ";");

            if (track != null)
                track.setStyle("-fx-background-color: " + backgroundColor + ";");

            if (incrementButton != null)
                incrementButton.setStyle("-fx-background-color: " + backgroundColor + ";");

            if (decrementButton != null)
                decrementButton.setStyle("-fx-background-color: " + backgroundColor + ";");
        }
    }

    private void setVisibleProperties()
    {
        log("Initializing container stuff");
        // Container stuff
        menuBar.setMaxHeight(22.0);
        messageField.setPromptText("Message");
        VBox.setMargin(userList, new Insets(4, 0, 4, 4));
        userListPane.setMaxWidth(450.0);
        userListPane.setPrefWidth(175.0);
        userListPane.setMinWidth(100.0);
        userList.setSpacing(3.0);
        messageHolder.setSpacing(3.0);
        messagePane.setPrefWidth(450.0);
        messagePane.setMinWidth(100.0);
        messagePane.setMaxWidth(Double.MAX_VALUE);
        messagePane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagePane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        userListPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagePane.setFitToWidth(true);
        messagePane.setPannable(false);
        messagePane.vvalueProperty().bind(messageHolder.heightProperty());
        column1Constraints.setHgrow(Priority.ALWAYS);
        column1Constraints.setHalignment(HPos.LEFT);
        column1Constraints.setFillWidth(true);
        column2Constraints.setHgrow(Priority.ALWAYS);
        column2Constraints.setHalignment(HPos.LEFT);
        column2Constraints.setFillWidth(true);
        column2Constraints.setMaxWidth(250.0);
        row1Constraints.setVgrow(Priority.NEVER);
        row1Constraints.setValignment(VPos.TOP);
        row1Constraints.setFillHeight(false);
        row2Constraints.setVgrow(Priority.ALWAYS);
        row2Constraints.setValignment(VPos.BOTTOM);
        row2Constraints.setFillHeight(true);
        row3Constraints.setVgrow(Priority.NEVER);
        row3Constraints.setValignment(VPos.BOTTOM);
        row3Constraints.setFillHeight(true);
        menuBar.setSpacing(uiFont * 0.33);
        mainContentHolder.setDividerPosition(0, 0.75);
        connectButton.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + uiAccentColor + ";");
        disconnectButton.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + uiAccentColor + ";");
        uiSettingsButton.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + uiAccentColor + ";");
        messageField.setStyle("-fx-font-size: " + messageFontSize + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + uiAccentColor + ";");
        menuBar.setStyle("-fx-background-color: " + uiAccentColor + ";");
        messagePane.setStyle("-fx-background-color: " + backgroundColor + ";" +
            "-fx-background: " + backgroundColor + ";" +
            "-fx-background-insets: 0, 1;");
        userListPane.setStyle("-fx-background-color: " + backgroundColor + ";" +
            "-fx-background: " + backgroundColor + ";" +
            "-fx-background-insets: 0;");
        mainContent.setStyle("-fx-background-color: " + backgroundColor + ";");
        mainContentHolder.setStyle("-fx-background-color: " + uiAccentColor + ";");

        root.getStylesheets().add("css/stylesheet.css");

        log("Populating the message holder");
        for (int count = 0 ; count <= 125 ; count++)
            messageHolder.getChildren().add(count, new Text(" "));
    }

    private void setInteractions()
    {
        log("Setting interactions");
        log("Setting main stage interactions");
        this.primaryStage.setOnCloseRequest(e -> log("Primary Stage Close"));

        log("Setting disconnectButton/ connectButton interactions");

        connectButton.setOnAction(e -> showConnectWindow());
        connectButton.setOnMouseEntered(e ->
        {
            connectButton.setStyle("-fx-background-color: " + highlightColor + ";" +
                "-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";");
        });
        connectButton.setOnMouseExited(e ->
        {
            connectButton.setStyle("-fx-background-color: " + uiAccentColor + ";" +
                "-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";");
        });

        disconnectButton.setOnAction(e -> disconnectFromChannel());
        disconnectButton.setOnMouseEntered(e ->
        {
            disconnectButton.setStyle("-fx-background-color: " + highlightColor + ";" +
                "-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";");
        });
        disconnectButton.setOnMouseExited(e ->
        {
            disconnectButton.setStyle("-fx-background-color: " + uiAccentColor + ";" +
                "-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";");
        });

        uiSettingsButton.setOnAction(e -> showUISettingsWindow());
        uiSettingsButton.setOnMouseEntered(e ->
        {
            uiSettingsButton.setStyle("-fx-background-color: " + highlightColor + ";" +
                "-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";");
        });
        uiSettingsButton.setOnMouseExited(e ->
        {
            uiSettingsButton.setStyle("-fx-background-color: " + uiAccentColor + ";" +
                "-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";");
        });

        messagePane.vvalueProperty().addListener((ObservableValue<? extends Number> obs, Number oldValue, Number newValue) ->
        {
            if (newValue.doubleValue() != 1.0)
                messagePane.vvalueProperty().unbind();

            else
                messagePane.vvalueProperty().bind(messageHolder.heightProperty());
        });

        log("setting message field interactions");
        messageField.setOnKeyPressed(event ->
        {
            if (event.getCode().equals(KeyCode.ENTER))
            {
                String message = messageField.getText().trim();
                if (message.length() > 0)
                {
                    if (connectedToChannel)
                    {
                        char[] rawMessage = message.toCharArray();
                        sendMessage("PRIVMSG " + Session.getChannel() + " :" + message);

                        if (! hasUserState)
                        {
                            displayMessage("> " + client.getNick() + " : " + message);
                        }

                        else
                        {
                            ArrayList<Image> clientImageBadges = null;
                            ArrayList<Node> messageNodes = new ArrayList<>();
                            StringBuilder sb = new StringBuilder();

                            if (session.getBadgeSignatures().size() >= 1)
                            {
                                clientImageBadges = new ArrayList<>();

                                for (String badge : session.getBadgeSignatures())
                                    clientImageBadges.add(Badges.getBadge(badge));
                            }

                            userList.addUser(client.getNick(), clientImageBadges);

                            int messageLength = message.length();
                            int index = 0;
                            for (char c : rawMessage)
                            {
                                index++;
                                if (c == 32)
                                {
                                    messageNodes.add(new Label(sb.toString()));
                                    sb = new StringBuilder();
                                    continue;
                                }

                                sb.append(c);

                                if (index == messageLength)
                                    messageNodes.add(new Label(sb.toString()));
                            }

                            displayMessage(DataProcessor.formatMessage(
                                clientImageBadges, session.getClientDisplayName(),
                                session.getClientColor(), messageNodes));
                        }
                    }
                    else
                    {
                        displayMessage("> You are not connected to a channel yet!");
                    }
                    messageField.clear();
                }
            }
        });
    }

    private void addNodesToParents()
    {
        log("Adding nodes to parents");
        menuBar.getChildren().addAll(connectButton, disconnectButton, uiSettingsButton);
        mainContent.getColumnConstraints().addAll(column1Constraints, column2Constraints);
        mainContent.getRowConstraints().addAll(row1Constraints, row2Constraints, row3Constraints);
        messagePane.setContent(messageHolder);
        userListPane.setContent(userList);
        mainContentHolder.getItems().setAll(messagePane, userListPane);
        mainContent.add(menuBar, 0, 0, 2, 1);
        mainContent.add(mainContentHolder, 0, 1, 2, 1);
        mainContent.add(messageField, 0, 2, 2, 1);
    }

    private void showConnectWindow()
    {
        BorderPane contentHolder = new BorderPane();
        VBox controller = new VBox();
        Label title = new Label("Enter a streamers name to join their channel");
        TextField streamerField = new TextField();
        streamerField.setPromptText("Streamer");
        Button confirmButton = new Button("Confirm"),
               cancelButton = new Button("Cancel");

        title.setStyle("-fx-text-fill: " + textFill + ";" +
            "-fx-font-size: " + uiFont + ";");

        streamerField.setStyle("-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + uiAccentColor + ";" +
            "-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill);
        streamerField.setOnKeyPressed(event ->
        {
            if (event.getCode().equals(KeyCode.ENTER))
                confirmButton.fire();
        });

        confirmButton.setStyle("-fx-background-color: " + uiAccentColor + ";" +
            "-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";");
        confirmButton.setOnMouseEntered(e ->
        {
            confirmButton.setStyle("-fx-background-color: " + highlightColor + ";" +
                "-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";");
        });
        confirmButton.setOnMouseExited(e ->
        {
            confirmButton.setStyle("-fx-background-color: " + uiAccentColor + ";" +
                "-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";");
        });
        confirmButton.setOnAction(e ->
        {
            String channel = streamerField.getText();

            if (connectedToChannel)
                disconnectFromChannel();

            if (! channel.substring(0, 1).contains("#"))
                channel = "#" + channel;

            channel = channel.toLowerCase();

            log("Connecting to " + channel);
            sendMessage("JOIN " + channel);

            session.setChannel(channel);
            displayMessage("> Joining channel " + Session.getChannel() + "...");
            cancelButton.fire();
        });

        cancelButton.setStyle("-fx-background-color: " + uiAccentColor + ";" +
            "-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";");
        cancelButton.setOnMouseEntered(e ->
        {
            cancelButton.setStyle("-fx-background-color: " + highlightColor + ";" +
                "-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";");
        });
        cancelButton.setOnMouseExited(e ->
        {
            cancelButton.setStyle("-fx-background-color: " + uiAccentColor + ";" +
                "-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";");
        });
        cancelButton.setOnAction(e ->
        {
            userListPane.setContent(userList);
            messagePane.setVvalue(1.0);
            messagePane.vvalueProperty().bind(messageHolder.heightProperty());
            messagePane.setContent(messageHolder);
        });

        streamerField.setMaxWidth(250.0);
        BorderPane.setAlignment(streamerField, Pos.CENTER);

        contentHolder.setCenter(streamerField);
        contentHolder.setTop(title);
        controller.getChildren().addAll(confirmButton, cancelButton);
        controller.minWidthProperty().bind(userListPane.widthProperty());
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(25, 0, 0, 0));

        for (Node btn : controller.getChildren())
            if (btn instanceof Button)
            {
                VBox.setMargin(btn, new Insets(7, 0, 0, 0));
                ((Button) btn).minWidthProperty().bind(controller.minWidthProperty());
            }

        contentHolder.prefHeightProperty().bind(messagePane.heightProperty());
        contentHolder.prefWidthProperty().bind(messagePane.widthProperty());
        userListPane.setContent(controller);
        messagePane.vvalueProperty().unbind();
        messagePane.setVvalue(0.0);
        messagePane.setContent(contentHolder);
        title.requestFocus();
    }

    // TODO: Implement
    private void askForCredentials()
    {
        Stage secondaryStage = new Stage();
        VBox contentHolder = new VBox();

        Text title = new Text("Please enter credentials");
        TextField userNameTextField = new TextField();
        userNameTextField.setPromptText("Username");
        TextField oauthTextField = new TextField();
        oauthTextField.setPromptText("OAUTH KEY");
        Button confirmButton = new Button("Confirm");

        contentHolder.setSpacing(20.0);
        contentHolder.setAlignment(Pos.CENTER);

        contentHolder.getChildren().addAll(title, userNameTextField, oauthTextField, confirmButton);

        confirmButton.setOnAction(e -> {
            try
            {
                client = new Client(oauthTextField.getText(), userNameTextField.getText());
            }
            catch (IllegalOauthKey y)
            {
                log("Bad OAUTH key entered");
                title.setText("Bad OAUTH key");
                return;
            }

            if (client.isReady())
            {
                Platform.runLater(() ->
                {
                    log("Recording client data");
                    ObjectOutputStream os = null;
                    try
                    {
                        os = new ObjectOutputStream(new FileOutputStream(new File(credentialFile)));
                        os.writeObject(client);
                        os.flush();
                        os.close();
                    } catch (IOException z)
                    {
                        // Something really fucked up.
                        log("I strangely did not find the credentials file...");
                    }
                });

                log("Correct client data entered");
                secondaryStage.close();
            }
            else
            {
                log("Incorrect client data");
                title.setText("You did not enter correct values!");
            }
        });

        oauthTextField.setOnKeyPressed(e ->
        {
            if (e.getCode().equals(KeyCode.ENTER))
                confirmButton.fire();
        });

        Scene root = new Scene(contentHolder, 400, 300);
        secondaryStage.setScene(root);
        secondaryStage.setTitle("Enter credentials");
        secondaryStage.setOnShown(e -> title.requestFocus());
        secondaryStage.setOnCloseRequest(e ->
        {
            if (client == null || ! client.isReady())
            {
                log("Credential window exit w/o client being initialized!");
                System.exit(1);
            }
        });
        secondaryStage.initStyle(StageStyle.UTILITY);
        secondaryStage.showAndWait();
    }
    // END TODO

    private void showUISettingsWindow()
    {
        messagePane.vvalueProperty().unbind();
        messagePane.setVvalue(0.0); // scroll to the top
        VBox dummy = new VBox();
        BorderPane titleHolder = new BorderPane();
        Label title = new Label("Application restart is required for all changes");
        titleHolder.setCenter(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(7, 0, 0, 0));
        dummy.getChildren().add(titleHolder);
        GridPane contentHolder = new GridPane();
        dummy.getChildren().add(contentHolder);
        VBox.setMargin(contentHolder, new Insets(10));
        VBox controller = new VBox();
        Button closeButton = new Button("Exit"),
               applyButton = new Button("Apply"),
               resetButton = new Button("Reset To Defaults"),
               shareUISettingsButton = new Button("Share Presets"),
               importUISettingsButton = new Button("Import Presets"),
               colorPickerButton = new Button("Color Picker");
        Label messageFontLabel = new Label("Message Font"),
              uiFontLabel = new Label("UI Font"),
              textColorLabel = new Label("Text Color"),
              backgroundColorLabel = new Label("Background Color"),
              highlightColorLabel = new Label("HighLight Color"),
              uiAccentColorLabel = new Label("UI Accent Color"),
              highlightTextColorLabel = new Label("Highlight Text Color");
        TextField messageFontInput = new TextField(String.valueOf((int) uiSettings.getMessageFontSize())),
                  uiFontInput = new TextField(String.valueOf((int) uiSettings.getUiFont())),
                  textColorInput = new TextField(textFill),
                  backgroundColorInput = new TextField(backgroundColor),
                  highlighColorInput = new TextField(highlightColor),
                  uiAccentColorInput = new TextField(uiAccentColor),
                  highlightTextColorInput = new TextField(highlightTextColor);
        Circle textColorCircle = new Circle(20.0),
               backgroundColorCircle = new Circle(20.0),
               highlighColorCircle = new Circle(20.0),
               uiAccentColorCircle = new Circle(20.0),
               highlightTextColorCircle = new Circle(20.0);

        textColorCircle.setStyle("-fx-fill: " + textFill + ";" +
            "-fx-stroke-width: 3px;" +
            "-fx-stroke: derive(" + textFill + ", 100%);");
        backgroundColorCircle.setStyle("-fx-fill: " + backgroundColor + ";" +
            "-fx-stroke-width: 3px;" +
            "-fx-stroke: derive(" + backgroundColor + ", 100%);");
        highlighColorCircle.setStyle("-fx-fill: " + highlightColor + ";" +
            "-fx-stroke-width: 3px;" +
            "-fx-stroke: derive(" + highlightColor + ", 100%);");
        uiAccentColorCircle.setStyle("-fx-fill: " + uiAccentColor + ";" +
            "-fx-stroke-width: 3px;" +
            "-fx-stroke: derive(" + uiAccentColor + ", 100%);");
        highlightTextColorCircle.setStyle("-fx-fill: " + highlightTextColor + ";" +
            "-fx-stroke-width: 3px;" +
            "-fx-stroke: derive(" + highlightTextColor + ", 100%);");

        messageFontInput.setMaxWidth(2.85 * uiFont);
        messageFontInput.setAlignment(Pos.CENTER_LEFT);
        messageFontInput.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + uiAccentColor + ";");
        uiFontInput.setMaxWidth(2.85 * uiFont);
        uiFontInput.setAlignment(Pos.CENTER_LEFT);
        uiFontInput.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + uiAccentColor + ";");
        textColorInput.setMaxWidth(6.1 * uiFont);
        textColorInput.setAlignment(Pos.CENTER_LEFT);
        textColorInput.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + uiAccentColor + ";");
        backgroundColorInput.setMaxWidth(6.1 * uiFont);
        backgroundColorInput.setAlignment(Pos.CENTER_LEFT);
        backgroundColorInput.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + uiAccentColor + ";");
        highlighColorInput.setMaxWidth(6.1 * uiFont);
        highlighColorInput.setAlignment(Pos.CENTER_LEFT);
        highlighColorInput.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + uiAccentColor + ";");
        uiAccentColorInput.setMaxWidth(6.1 * uiFont);
        uiAccentColorInput.setAlignment(Pos.CENTER_LEFT);
        uiAccentColorInput.setStyle("-fx-font-Size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";" +
                "-fx-background-color: " + backgroundColor + ";" +
                "-fx-border-color: " + uiAccentColor + ";");
        highlightTextColorInput.setMaxWidth(6.1 * uiFont);
        highlightTextColorInput.setAlignment(Pos.CENTER_LEFT);
        highlightTextColorInput.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color:  " + uiAccentColor + ";");

        messageFontLabel.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";");
        uiFontLabel.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";");
        textColorLabel.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";");
        backgroundColorLabel.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";");
        highlightColorLabel.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";");
        uiAccentColorLabel.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";");
        highlightTextColorLabel.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";");

        title.setStyle("-fx-text-fill: " + textFill + ";" +
            "-fx-font-size: " + uiFont + ";");

        closeButton.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + uiAccentColor + ";");
        closeButton.setOnMouseEntered(e ->
        {
            closeButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";" +
                "-fx-background-color: " + highlightColor + ";");
        });
        closeButton.setOnMouseExited(e ->
        {
            closeButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";" +
                "-fx-background-color: " + uiAccentColor + ";");
        });
        closeButton.setOnAction(e ->
        {
            messagePane.setVvalue(1.0); // scroll to the bottom
            messagePane.vvalueProperty().bind(messageHolder.heightProperty());
            messagePane.setContent(messageHolder);
            userListPane.setContent(userList);
        });

        applyButton.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + uiAccentColor + ";");
        applyButton.setOnMouseEntered(e ->
        {
            applyButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";" +
                "-fx-background-color: " + highlightColor + ";");
        });
        applyButton.setOnMouseExited(e ->
        {
            applyButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";" +
                "-fx-background-color: " + uiAccentColor + ";");
        });
        applyButton.setOnAction(e ->
        {
            uiSettings.setMessageFontSize(Double.valueOf(messageFontInput.getText()));
            uiSettings.setUiFont(Double.valueOf(uiFontInput.getText()));
            uiSettings.setTextFill(textColorInput.getText());
            uiSettings.setBackgroundColor(backgroundColorInput.getText());
            uiSettings.setHighlightColor(highlighColorInput.getText());
            uiSettings.setUIAccentColor(uiAccentColorInput.getText());
            uiSettings.setHighlightTextColor(highlightTextColorInput.getText());

            log("recording ui setting data");

            try
            {
                ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(new File(uiSettingsFile)));
                os.writeObject(uiSettings);
                os.close();
            }
            catch (IOException y)
            {
                log(y.getMessage());
            }
        });

        resetButton.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + uiAccentColor + ";");
        resetButton.setOnMouseEntered(e ->
        {
            resetButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";" +
                "-fx-background-color: " + highlightColor + ";");
        });
        resetButton.setOnMouseExited(e ->
        {
            resetButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";" +
                "-fx-background-color: " + uiAccentColor + ";");
        });
        resetButton.setOnAction(e ->
        {
            UISettings newSettings = new UISettings();

            try
            {
                ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(uiSettingsFile));
                os.writeObject(newSettings);
                os.close();
            }
            catch (IOException y)
            {
                log(y.getMessage());
            }

            messageFontInput.setText(String.valueOf((int) newSettings.getMessageFontSize()));
            uiFontInput.setText(String.valueOf((int) newSettings.getUiFont()));
            textColorInput.setText(newSettings.getTextFill());
            backgroundColorInput.setText(newSettings.getBackgroundColor());
            highlighColorInput.setText(newSettings.getHighlightColor());
            uiAccentColorInput.setText(newSettings.getUIAccentColor());
            highlightTextColorInput.setText(newSettings.getHighlightTextColor());
        });

        shareUISettingsButton.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + uiAccentColor + ";");
        shareUISettingsButton.setOnMouseEntered(e ->
        {
            shareUISettingsButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";" +
                "-fx-background-color: " + highlightColor + ";");
        });
        shareUISettingsButton.setOnMouseExited(e ->
        {
            shareUISettingsButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";" +
                "-fx-background-color: " + uiAccentColor + ";");
        });
        shareUISettingsButton.setOnAction(e ->
        {
            Stage stage = new Stage();
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("uisettings.dat");
            File saveToFile = fc.showSaveDialog(stage);

            if (saveToFile != null)
            {
                UISettings newSettings = new UISettings();
                newSettings.setMessageFontSize(Double.valueOf(messageFontInput.getText()));
                newSettings.setUiFont(Double.valueOf(uiFontInput.getText()));
                newSettings.setTextFill(textColorInput.getText());
                newSettings.setBackgroundColor(backgroundColorInput.getText());
                newSettings.setHighlightColor(highlighColorInput.getText());
                newSettings.setUIAccentColor(uiAccentColorInput.getText());
                newSettings.setHighlightTextColor(highlightTextColorInput.getText());

                try
                {
                    ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(saveToFile));
                    os.writeObject(newSettings);
                    os.close();
                }
                catch (IOException y)
                {
                    log(y.getMessage());
                }

            }
        });

        importUISettingsButton.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + uiAccentColor + ";");
        importUISettingsButton.setOnMouseEntered(e ->
        {
            importUISettingsButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";" +
                "-fx-background-color: " + highlightColor + ";");
        });
        importUISettingsButton.setOnMouseExited(e ->
        {
            importUISettingsButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";" +
                "-fx-background-color: " + uiAccentColor + ";");
        });
        importUISettingsButton.setOnAction(e ->
        {
            Stage stage = new Stage();
            FileChooser fc = new FileChooser();
            File importedObjectFile = fc.showOpenDialog(stage);

            if (importedObjectFile != null)
            {
                UISettings newSettings = null;
                try
                {
                    ObjectInputStream is = new ObjectInputStream(new FileInputStream(importedObjectFile));
                    newSettings = (UISettings) is.readObject();
                    is.close();
                }
                catch (IOException | ClassNotFoundException y)
                {
                    log(y.getMessage());
                }
                if (newSettings != null)
                {
                    uiSettings.setMessageFontSize(newSettings.getMessageFontSize());
                    uiSettings.setUiFont(newSettings.getUiFont());
                    uiSettings.setTextFill(newSettings.getTextFill());
                    uiSettings.setBackgroundColor(newSettings.getBackgroundColor());
                    uiSettings.setHighlightColor(newSettings.getHighlightColor());
                    uiSettings.setUIAccentColor(newSettings.getUIAccentColor());
                    uiSettings.setHighlightTextColor(newSettings.getHighlightTextColor());
                    messageFontInput.setText(String.valueOf((int) uiSettings.getMessageFontSize()));
                    uiFontInput.setText(String.valueOf((int) uiSettings.getUiFont()));
                    textColorInput.setText(uiSettings.getTextFill());
                    backgroundColorInput.setText(uiSettings.getBackgroundColor());
                    highlighColorInput.setText(uiSettings.getHighlightColor());
                    uiAccentColorInput.setText(uiSettings.getUIAccentColor());
                    highlightTextColorInput.setText(uiSettings.getHighlightTextColor());
                }
            }
        });

        colorPickerButton.setStyle("-fx-font-size: " + uiFont + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-background-color: " + uiAccentColor + ';');
        colorPickerButton.setOnMouseEntered(e ->
        {
            colorPickerButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + highlightTextColor + ";" +
                "-fx-background-color: " + highlightColor + ';');
        });
        colorPickerButton.setOnMouseExited(e ->
        {
            colorPickerButton.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";" +
                "-fx-background-color: " + uiAccentColor + ';');
        });
        colorPickerButton.setOnAction(e ->
        {
            getHostServices().showDocument("https://duckduckgo.com/?q=color+Picker&ia=answer");
        });

        messageFontLabel.setAlignment(Pos.CENTER_RIGHT);
        uiFontLabel.setAlignment(Pos.CENTER_RIGHT);

        contentHolder.setVgap(7.0);
        contentHolder.setHgap(15.0);
        contentHolder.add(messageFontLabel, 0, 0);
        contentHolder.add(messageFontInput, 1, 0);
        contentHolder.add(uiFontLabel, 0, 1);
        contentHolder.add(uiFontInput, 1, 1);
        contentHolder.add(textColorLabel, 0, 2);
        contentHolder.add(textColorInput, 1, 2);
        contentHolder.add(textColorCircle, 2, 2);
        contentHolder.add(backgroundColorLabel, 0, 3);
        contentHolder.add(backgroundColorInput, 1, 3);
        contentHolder.add(backgroundColorCircle, 2, 3);
        contentHolder.add(highlightColorLabel, 0, 4);
        contentHolder.add(highlighColorInput, 1, 4);
        contentHolder.add(highlighColorCircle, 2, 4);
        contentHolder.add(uiAccentColorLabel, 0, 5);
        contentHolder.add(uiAccentColorInput, 1, 5);
        contentHolder.add(uiAccentColorCircle, 2, 5);
        contentHolder.add(highlightTextColorLabel, 0, 6);
        contentHolder.add(highlightTextColorInput, 1, 6);
        contentHolder.add(highlightTextColorCircle, 2, 6);
        controller.getChildren().addAll(closeButton, applyButton, resetButton,
            shareUISettingsButton, importUISettingsButton, colorPickerButton);

        controller.setAlignment(Pos.TOP_RIGHT);
        controller.minWidthProperty().bind(userListPane.widthProperty());
        contentHolder.minWidthProperty().bind(messagePane.widthProperty());

        for (Node btn : controller.getChildren())
            if (btn instanceof Button)
            {
                VBox.setMargin(btn, new Insets(7, 0, 0, 0));
                ((Button) btn).prefWidthProperty().bind(controller.minWidthProperty());
            }


        messagePane.setContent(dummy);
        userListPane.setContent(controller);

        messageFontInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.isEmpty())
                ((StringProperty)obs).setValue(newVal);

            else if (newVal.matches("\\d+") && Integer.parseInt(newVal) <= 35)
                ((StringProperty)obs).setValue(newVal);

            else
                ((StringProperty)obs).setValue(oldVal);
        });
        uiFontInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.isEmpty())
                ((StringProperty)obs).setValue(newVal);

            else if (newVal.matches("\\d+") && Integer.parseInt(newVal) <= 35)
                ((StringProperty)obs).setValue(newVal);

            else
                ((StringProperty)obs).setValue(oldVal);
        });
        textColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty)obs).setValue(newVal);
                if (newVal.length() == 7)
                    textColorCircle.setFill(Paint.valueOf(newVal));
            }
            else
            {
                ((StringProperty)obs).setValue(oldVal);
            }
        });
        backgroundColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty)obs).setValue(newVal);
                if (newVal.length() == 7)
                    backgroundColorCircle.setFill(Paint.valueOf(newVal));
            }
            else
            {
                ((StringProperty)obs).setValue(oldVal);
            }
        });
        highlighColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty)obs).setValue(newVal);
                if (newVal.length() == 7)
                    highlighColorCircle.setFill(Paint.valueOf(newVal));
            }
            else
            {
                ((StringProperty)obs).setValue(oldVal);
            }
        });
        uiAccentColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty)obs).setValue(newVal);
                if (newVal.length() == 7)
                    uiAccentColorCircle.setFill(Paint.valueOf(newVal));
            }
            else
            {
                ((StringProperty)obs).setValue(oldVal);
            }
        });
        highlightTextColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty)obs).setValue(newVal);
                if (newVal.length() == 7)
                    highlightTextColorCircle.setFill(Paint.valueOf(newVal));
            }
            else
            {
                ((StringProperty)obs).setValue(oldVal);
            }
        });

    }

    private void disconnectFromChannel()
    {
        if (connectedToChannel)
        {
            log("Disconnecting from " + Session.getChannel());
            sendMessage("PART " + Session.getChannel());
            displayMessage("> Leaving channel " + Session.getChannel());
            session = new Session();
            userList.removeAllUsers();
            hasUserState = false;
            connectedToChannel = false;
        }
        else
        {
            displayMessage("> You are not connected to a channel!");
        }
    }

    private void sendMessage(String message) { socketRunner.sendMessage(message.trim()); }

    static void displayMessage(String message)
    {
        Label newMessage = new Label(message);
        newMessage.setWrapText(true);
        newMessage.setFont(Font.font(messageFontSize));
        newMessage.setTextFill(Paint.valueOf(textFill));
        newMessage.prefWidthProperty().bind(messagePane.widthProperty());

        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(125, newMessage);
    }

    static void displayMessage(FlowPane holder)
    {
        holder.maxWidthProperty().bind(messagePane.widthProperty());

        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(125, holder);
    }

    public static void main(String[] args)
    {
        launchArgs = args;
        launch(args);
    }
}
