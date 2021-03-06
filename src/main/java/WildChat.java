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

import UISettings.UISettings;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

import static logUtils.Logger.log;
import static logUtils.Logger.setShouldLog;

public class WildChat extends Application
{
    static Client client = null;
    static Session session = new Session();
    static UserList userList = new UserList();
    static String textFill, backgroundColor, highlightColor,
            uiAccentColor, highlightTextColor, actionColor;
    static double messageFontSize, uiFont;
    static volatile boolean connected = false,
            connectionMessageReceived = false,
            connectedToChannel = false,
            credentialsAvailable = false,
            hasUserState = false;
    private static String[] launchArgs;
    private static Stage primaryStage = null;
    private static ScrollPane messagePane = new ScrollPane(),
            userListPane = new ScrollPane();
    private static VBox messageHolder = new VBox();
    private final String filePrefix = ".WildChat/",
            credentials = "credentials.dat",
            uiSettingsFileName = "uisettings.dat",
            dotDirLocation = (System.getProperty("os.name").contains("Windows"))
                             ? BasicIO.getEnvVars("APPDATA") + "/"
                             : BasicIO.getEnvVars("HOME") + "/",
            credentialFile = dotDirLocation + filePrefix + credentials,
            uiSettingsFile = dotDirLocation + filePrefix + uiSettingsFileName;
    static String VERSION;
    private Scene root;
    private Thread baseConnectionThread = null;
    static UISettings uiSettings = new UISettings();
    private TwitchConnect socketRunner = null;
    private SplitPane mainContentHolder = new SplitPane();
    private GridPane mainContent = new GridPane();
    private HBox menuBar = new HBox();
    private Button connectButton = new Button("Connect"),
            uiSettingsButton = new Button("Customize UI"),
            disconnectButton = new Button("Disconnect"),
            aboutButton = new Button("About");
    private TextField messageField = new TextField();
    private ColumnConstraints column1Constraints = new ColumnConstraints(),
            column2Constraints = new ColumnConstraints();
    private RowConstraints row1Constraints = new RowConstraints(),
            row2Constraints = new RowConstraints(),
            row3Constraints = new RowConstraints();
    private String initialChannel = null;
    static SimpleStringProperty title = new SimpleStringProperty();

    public WildChat()
    {
        String version = null;
        BufferedReader versionReader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("text/version.txt")
                )
        );
        try
        {
            version = versionReader.readLine();
            versionReader.close();
        } catch (IOException e)
        {
            version = "BETA BUILD";
            log(e.getMessage());
        } finally
        {
            VERSION = version;
        }

        boolean canAccessCredentials,
                canAccessUiSettings,
                debug = true;

        if (launchArgs.length > 0)
        {
            for (String arg : launchArgs)
            {
                if (arg.equals("--no-debug"))
                {
                    debug = false;
                } else if (arg.contains("--channel="))
                {
                    initialChannel = "#" + arg.substring(arg.indexOf('=') + 1);
                }
            }
        }

        setShouldLog(debug);

        log("Testing read/write on " + credentialFile);
        if (!FileUtil.canReadWrite(credentialFile))
        {
            log("File " + credentialFile + " either does not exist or can't be read/wrote to");
            log("Attempting to create necessary dirs and file for " + credentialFile);
            canAccessCredentials = FileUtil.createFileWithDirs(credentialFile);
        } else
            canAccessCredentials = true;

        log((canAccessCredentials)
            ? "Can read/write " + credentialFile
            : "Can't read/write " + credentialFile);

        if (credentialsAvailable = FileUtil.hasData(credentialFile))
        {
            log("Credential file has data");
            try
            {
                ObjectInputStream is = new ObjectInputStream(new FileInputStream(new File(credentialFile)));
                client = (Client) is.readObject();
                is.close();
            } catch (IOException | ClassNotFoundException e)
            {
                log("Unrecoverable credential read operation");
                return;
            }
        }
        log((credentialsAvailable) ? "Credentials read in" : "No credential data found");

        log("Testing read/write on " + uiSettingsFile);
        if (!FileUtil.canReadWrite(uiSettingsFile))
        {
            log("File " + uiSettingsFile + " either does not exist or can't be read/wrote to");
            log("Attempting to create necessary dirs and file for " + uiSettingsFile);
            canAccessUiSettings = FileUtil.createFileWithDirs(uiSettingsFile);
        } else
            canAccessUiSettings = true;

        log((canAccessUiSettings)
            ? "Can read/write " + uiSettingsFile
            : "Can't read/write " + uiSettingsFile);

        if (FileUtil.hasData(uiSettingsFile))
        {
            log("UISettings file has data");
            File uiSettingFile = new File(uiSettingsFile);
            try
            {
                ObjectInputStream is = new ObjectInputStream(new FileInputStream(uiSettingFile));
                uiSettings = (UISettings) is.readObject();
                is.close();
            } catch (IOException | ClassNotFoundException e)
            {
                log("Failed to import UISettings... Assuming corrupt file, deleting");
                uiSettingFile.delete();
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
        actionColor = uiSettings.getActionColor();
    }

    private void clearMessageArea()
    {
        log("Populating the message holder");
        messageHolder.getChildren().clear();

        for (int count = 0; count <= 200; count++)
        {
            Text dummy = new Text(" ");
            dummy.setCache(true);
            messageHolder.getChildren().add(count, dummy);
        }
    }

    static void styleCircle(Circle circleToStyle, String color)
    {
        circleToStyle.setStyle("-fx-fill: " + color + ";" +
                "-fx-stroke-width: 3px;" +
                "-fx-stroke: derive(" + color + ", 100%)");
    }

    static void styleTextField(TextField textFieldToStyle, double scale)
    {
        textFieldToStyle.setMaxWidth(scale * uiFont);
        textFieldToStyle.setAlignment(Pos.CENTER_LEFT);
        textFieldToStyle.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";" +
                "-fx-background-color: " + backgroundColor + ";" +
                "-fx-border-color: " + uiAccentColor + ";");
    }

    static void styleTextField(TextField textFieldToStyle)
    {
        textFieldToStyle.setAlignment(Pos.CENTER_LEFT);
        textFieldToStyle.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";" +
                "-fx-background-color: " + backgroundColor + ";" +
                "-fx-border-color: " + uiAccentColor + ";");
    }

    static void styleUILabel(Label labelToStyle)
    {
        labelToStyle.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";");
    }

    static void styleButton(Button buttonToStyle)
    {
        buttonToStyle.setStyle("-fx-font-size: " + uiFont + ";" +
                "-fx-text-fill: " + textFill + ";" +
                "-fx-background-color: " + uiAccentColor + ";");

        buttonToStyle.setOnMouseEntered(e ->
        {
            primaryStage.getScene().setCursor(Cursor.HAND);
            buttonToStyle.setStyle("-fx-font-size: " + uiFont + ";" +
                    "-fx-text-fill: " + highlightTextColor + ";" +
                    "-fx-background-color: " + highlightColor + ";");
        });

        buttonToStyle.setOnMouseExited(e ->
        {
            primaryStage.getScene().setCursor(Cursor.DEFAULT);
            buttonToStyle.setStyle("-fx-font-size: " + uiFont + ";" +
                    "-fx-text-fill: " + textFill + ";" +
                    "-fx-background-color: " + uiAccentColor + ";");
        });
    }

    static void styleScrollBar(ScrollBar scrollBarToStyle)
    {
        Node thumb = scrollBarToStyle.lookup(".thumb");
        Node track = scrollBarToStyle.lookup(".track");
        Node incrementButton = scrollBarToStyle.lookup(".increment");
        Node decrementButton = scrollBarToStyle.lookup(".decrement");

        if (thumb != null)
        { thumb.setStyle("-fx-background-color: " + uiAccentColor + ";"); }

        if (track != null)
        { track.setStyle("-fx-background-color: " + backgroundColor + ";"); }

        if (incrementButton != null)
        { incrementButton.setStyle("-fx-background-color: " + backgroundColor + ";"); }

        if (decrementButton != null)
        { decrementButton.setStyle("-fx-background-color: " + backgroundColor + ";"); }
    }

    static void writeUISettingsToFile(UISettings settingsToWrite, File saveToFile)
    {
        try
        {
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(saveToFile));
            os.writeObject(settingsToWrite);
            os.close();
        } catch (IOException y)
        {
            log(y.getMessage());
        }
    }

    static void writeUISettingsToFile(UISettings settingsToWrite, String saveToFile)
    {
        writeUISettingsToFile(settingsToWrite, new File(saveToFile));
    }

    static void displayMessage(String message)
    {
        Label newMessage = new Label(uiSettings.getMessagePrefix() + message);
        newMessage.setWrapText(true);
        newMessage.setStyle("-fx-font-size: " + messageFontSize + ";" +
                "-fx-text-fill: " + textFill + ";");
        newMessage.prefWidthProperty().bind(messagePane.widthProperty());

        newMessage.setCache(true);

        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(200, newMessage);
    }

    static void displayMessage(FlowPane holder)
    {
        holder.maxWidthProperty().bind(messagePane.widthProperty());
        holder.setCache(true);

        messageHolder.getChildren().remove(0);
        messageHolder.getChildren().add(200, holder);
    }
    // END TODO

    public static void main(String[] args)
    {
        launchArgs = args;
        launch(args);
    }

    @Override
    public void stop()
    {
        if (credentialsAvailable)
        {
            log("Recording client data");
            try
            {
                ObjectOutputStream os = new ObjectOutputStream(
                        new FileOutputStream(
                                new File(credentialFile)
                        )
                );
                os.writeObject(client);
                os.flush();
                os.close();
            } catch (IOException z)
            {
                // Something really fucked up.
                log("I strangely did not find the credentials file...");
            }
        }

        log("Correct client data entered");
        connected = false;
        log("ShutDown");
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage)
    {
        log("Stage start");
        WildChat.primaryStage = primaryStage;

        log((!credentialsAvailable) ? "No credentials available, showing input fields" : "");
        if (!credentialsAvailable)
        { askForCredentials(); }

        if (client == null)
        {
            log("Client not initialized!");
            System.exit(1);
        }

        log("Setting up networking");
        socketRunner = new TwitchConnect(client, initialChannel);

        log("Setting scene");
        root = new Scene(mainContent, uiSettings.getWindowWidth(), uiSettings.getWindowHeight());

        log("initializing UI");
        initUI();

        WildChat.primaryStage.setScene(root);
        title.setValue("WildChat - " + VERSION);
        WildChat.primaryStage.titleProperty().bind(title);

        // CTRL+Q exit application
        WildChat.primaryStage.getScene().getAccelerators().put(
                KeyCombination.keyCombination("CTRL+Q"), this::stop
        );

        log("Showing window");
        WildChat.primaryStage.show();

        // call post shown ui props
        initPostUI();

        log("Starting networking");
        baseConnectionThread = new Thread(socketRunner);
        baseConnectionThread.start();
        log("Networking started");
        displayMessage("Connecting to twitch.tv...");
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
        Node messagePaneScrollBar = messagePane.lookup(".scroll-bar");
        Node userListPaneScrollBar = userListPane.lookup(".scroll-bar");

        if (divider != null)
        { divider.setStyle("-fx-background-color: " + uiAccentColor + ";"); }

        if (messagePaneScrollBar != null)
        { styleScrollBar((ScrollBar) messagePaneScrollBar); }

        if (userListPaneScrollBar != null)
        { styleScrollBar((ScrollBar) userListPaneScrollBar); }
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
        styleButton(connectButton);
        styleButton(disconnectButton);
        styleButton(uiSettingsButton);
        styleButton(aboutButton);
        styleTextField(messageField);
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

        primaryStage.getIcons().addAll(
                new Image(WildChat.class.getResourceAsStream("icons/wildchat_logo_1024.png")),
                new Image(WildChat.class.getResourceAsStream("icons/wildchat_logo_512.png")),
                new Image(WildChat.class.getResourceAsStream("icons/wildchat_logo_256.png")),
                new Image(WildChat.class.getResourceAsStream("icons/wildchat_logo_128.png")),
                new Image(WildChat.class.getResourceAsStream("icons/wildchat_logo_64.png")),
                new Image(WildChat.class.getResourceAsStream("icons/wildchat_logo_32.png")),
                new Image(WildChat.class.getResourceAsStream("icons/wildchat_logo_16.png"))
        );

        clearMessageArea();

    }

    private void setInteractions()
    {
        log("Setting interactions");
        log("Setting main stage interactions");
        primaryStage.setOnCloseRequest(e -> log("Primary Stage Close"));

        log("Setting disconnectButton/ connectButton interactions");

        connectButton.setOnAction(e ->
        {
            if (connectionMessageReceived)
                showConnectWindow();

            else
                displayMessage("Not connected to twitch.tv yet!");
        });
        disconnectButton.setOnAction(e -> disconnectFromChannel());
        uiSettingsButton.setOnAction(e -> showUISettingsWindow());

        messagePane.vvalueProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.doubleValue() != 1.0)
                messagePane.vvalueProperty().unbind();

            else
                messagePane.vvalueProperty().bind(messageHolder.heightProperty());
        });

        aboutButton.setOnAction(e -> showAboutWindow());

        mainContent.addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            if (event.getCode() == KeyCode.TAB)
            {
                if (event.getTarget() instanceof TextField)
                {
                    event.consume();

                    log("TAB pressed");
                    String message = messageField.getText().trim();
                    if (message.length() > 1)
                    {
                        log("Message: " + message);
                        int lastWordIndex = message.lastIndexOf(" ") + 1;
                        String word;

                        if (lastWordIndex < 0)
                            return;

                        if (lastWordIndex == 0)
                        {
                            word = message;
                        } else
                        {
                            word = message.substring(lastWordIndex);
                        }

                        log("Last word: " + word);

                        if (!word.startsWith("@"))
                            return;

                        int wordLen = word.length() - 1;
                        log("last word len: " + wordLen);
                        for (String user : userList.getUsers())
                        {
                            String charsToMatch = user.substring(0, wordLen);
                            charsToMatch = "@" + charsToMatch;
                            log("User to match: " + charsToMatch);

                            if (charsToMatch.equals(word))
                            {
                                log("MATCH!");
                                message = message.substring(0, lastWordIndex);
                                message += "@" + user;
                                messageField.setText(message);
                                messageField.positionCaret(message.length());
                            }
                        }
                    }
                }
            }
        });

        log("Setting message field interactions");
        messageField.setOnKeyPressed(event ->
        {
            if (event.getCode().equals(KeyCode.ENTER))
            {
                if (connectionMessageReceived)
                {
                    String message = messageField.getText().trim();
                    if (message.length() > 0)
                    {
                        if (message.startsWith("/")) {
                            if (message.length() >= 2)
                            {
                                // test for /clear before /c
                                if (message.length() >= 6) {
                                    if (message.substring(0, 6).equals("/clear"))
                                    {
                                        clearMessageArea();
                                        messageField.clear();
                                        return;
                                    }
                                }

                                if (message.substring(0, 2).equals("/c"))
                                {
                                    connectButton.fire();
                                    messageField.clear();
                                    return;
                                } else if (message.substring(0, 2).equals("/d"))
                                {
                                    disconnectButton.fire();
                                    messageField.clear();
                                    return;
                                }
                            }
                        }

                        if (!connectedToChannel)
                        {
                            displayMessage("You are not connected to a channel yet!");
                            messageField.clear();
                            return;
                        }

                        sendMessage("PRIVMSG " + Session.getChannel() + " :" + message);
                    if (!hasUserState)
                        {
                            displayMessage(client.getNick() + " : " + message);
                        } else
                        {
                            boolean isWhisp = false;

                            if (message.length() >= 2)
                            {
                                if (message.substring(0, 2).equals("/w")) 
                                    isWhisp = true;
                            }
                            if (isWhisp)
                            {
                                int userStartIndex = message.indexOf(" ") + 1;
                                int endUserStart = message.indexOf(" ", userStartIndex);
                                String receivingUser = message.substring(userStartIndex, endUserStart);
                                String whisperMessage = message.substring(endUserStart + 1);

                                ArrayList<Node> messageNodes = new ArrayList<>();
                                StringBuilder sb = new StringBuilder();
                                char[] rawMessage = whisperMessage.toCharArray();

                                int messageLength = whisperMessage.length();
                                int index = 0;
                                for (char c : rawMessage)
                                {
                                    index++;
                                    if (c == 32 || index == messageLength)
                                    {
                                        if (index == messageLength)
                                        {
                                            sb.append(c);
                                        }

                                        String word = sb.toString();
                                        if (session.getEmoteCodesAndIDs()
                                                .containsKey(word))
                                        {
                                            log("Found emote");
                                            String emoteID =
                                                    session.getEmoteCodesAndIDs().get(word);
                                            log(emoteID);
                                            if (Emotes.hasEmote(emoteID))
                                            {
                                                messageNodes.add(new ImageView(
                                                        Emotes.getEmote(emoteID)));
                                            } else
                                            {
                                                Image emote = new Image(
                                                        String.format(
                                                                HandleData.EMOTE_DOWNLOAD_URL,
                                                                emoteID),
                                                        true
                                                );
                                                Emotes.cacheEmote(emote, emoteID);
                                                messageNodes.add(new ImageView(emote));
                                            }
                                        } else
                                        {
                                            messageNodes.add(new Label(sb.toString()));
                                        }
                                        sb = new StringBuilder();
                                        continue;
                                    }
                                    sb.append(c);
                                }

                                FlowPane result = DataProcessor.formatMessage(null,
                                        "Whispering " + receivingUser, null, messageNodes,
                                        true);

                                displayMessage(result);
                            } else
                            {

                                ArrayList<Image> clientImageBadges = null;
                                ArrayList<Node> messageNodes = new ArrayList<>();
                                StringBuilder sb = new StringBuilder();
                                char[] rawMessage = message.toCharArray();

                                if (session.getBadgeSignatures().size() >= 1)
                                {
                                    clientImageBadges = new ArrayList<>();

                                    for (String badge : session.getBadgeSignatures())
                                    {
                                        clientImageBadges.add(Badges.getBadge(badge));
                                    }
                                }

                                userList.addUser(client.getNick(), clientImageBadges);

                                int messageLength = message.length();
                                int index = 0;
                                for (char c : rawMessage)
                                {
                                    index++;
                                    if (c == 32 || index == messageLength)
                                    {
                                        if (index == messageLength)
                                        {
                                            sb.append(c);
                                        }

                                        String word = sb.toString();
                                        if (session.getEmoteCodesAndIDs()
                                                .containsKey(word))
                                        {
                                            String emoteID =
                                                    session.getEmoteCodesAndIDs().get(word);
                                            log(emoteID);
                                            if (Emotes.hasEmote(emoteID))
                                            {
                                                messageNodes.add(new ImageView(
                                                        Emotes.getEmote(emoteID)));
                                            } else
                                            {
                                                Image emote = new Image(
                                                        String.format(
                                                                HandleData.EMOTE_DOWNLOAD_URL,
                                                                emoteID),
                                                        true
                                                );
                                                Emotes.cacheEmote(emote, emoteID);
                                                messageNodes.add(new ImageView(emote));
                                            }
                                        } else
                                        {
                                            messageNodes.add(new Label(sb.toString()));
                                        }
                                        sb = new StringBuilder();
                                        continue;
                                    }
                                    sb.append(c);
                                }

                                FlowPane result = DataProcessor.formatMessage(
                                        clientImageBadges, session.getClientDisplayName(),
                                        session.getClientColor(), messageNodes, false);

                                displayMessage(result);
                            }
                        }
                    }
                } else
                {
                    displayMessage("Not connected to twitch.tv yet!");
                }
                log("reached the end :(");
                messageField.clear();
            }
        });
    }

    private void addNodesToParents()
    {
        log("Adding nodes to parents");
        menuBar.getChildren().addAll(connectButton, disconnectButton, uiSettingsButton, aboutButton);
        mainContent.getColumnConstraints().addAll(column1Constraints, column2Constraints);
        mainContent.getRowConstraints().addAll(row1Constraints, row2Constraints, row3Constraints);
        messagePane.setContent(messageHolder);
        userListPane.setContent(userList);
        mainContentHolder.getItems().setAll(messagePane, userListPane);
        mainContent.add(menuBar, 0, 0, 2, 1);
        mainContent.add(mainContentHolder, 0, 1, 2, 1);
        mainContent.add(messageField, 0, 2, 2, 1);
    }

    private void showAboutWindow()
    {
        BorderPane contentHolder = new BorderPane();
        VBox controller = new VBox(), holder = new VBox();
        Label title = new Label("About WildChat"),
                githubPage = new Label("https://github.com/AWildBeard/WildChat"),
                verNum = new Label(VERSION),
                contributors = new Label("(Alphabetically) Contributors:");
        Button exitButton = new Button("Back");
        Scanner fileReader = new Scanner(getClass().getResourceAsStream("text/contributors.txt"));
        ImageView logoView = new ImageView(
                new Image(
                        WildChat.class.getResourceAsStream("icons/wildchat_logo_128.png")
                )
        );

        // This has to be here to maintain the way things are supposed to appear
        holder.getChildren().addAll(title, githubPage, logoView, verNum, contributors);

        while (fileReader.hasNextLine())
        { holder.getChildren().add(new Label(fileReader.nextLine())); }

        exitButton.setOnAction(e ->
        {
            userListPane.setContent(userList);
            messagePane.vvalueProperty().bind(messageHolder.heightProperty());
            messagePane.setContent(messageHolder);
            messageField.setDisable(false);
        });
        githubPage.setOnMouseClicked(e ->
                BareBonesBrowserLaunch.openURL("https://github.com/AWildBeard/WildChat")
        );
        githubPage.setOnMouseEntered(event -> primaryStage.getScene().setCursor(Cursor.HAND));
        githubPage.setOnMouseExited(event -> primaryStage.getScene().setCursor(Cursor.DEFAULT));

        for (Node label : holder.getChildren())
        {
            if (label instanceof Label)
            {
                styleUILabel((Label) label);
                ((Label) label).setAlignment(Pos.CENTER);
            }
        }

        githubPage.setStyle("-fx-text-fill: dodgerblue;" +
                "-fx-underline: true;" +
                "-fx-font-size: " + uiFont + ";");
        holder.setSpacing(30.0);
        holder.setAlignment(Pos.CENTER);
        styleUILabel(title);
        BorderPane.setMargin(holder, new Insets(45, 0, 45, 0));
        title.setAlignment(Pos.CENTER);
        BorderPane.setAlignment(holder, Pos.CENTER);
        styleButton(exitButton);

        controller.getChildren().add(exitButton);
        contentHolder.setCenter(holder);
        exitButton.minWidthProperty().bind(controller.minWidthProperty());
        VBox.setMargin(exitButton, new Insets(7, 0, 0, 0));
        controller.minWidthProperty().bind(userListPane.widthProperty());
        contentHolder.minWidthProperty().bind(messagePane.widthProperty());
        contentHolder.minHeightProperty().bind(messagePane.heightProperty());

        userListPane.setContent(controller);
        messagePane.vvalueProperty().unbind();
        messagePane.setVvalue(0.0);
        messagePane.setContent(contentHolder);
        messageField.setDisable(true);
        fileReader.close();
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

        styleUILabel(title);

        styleTextField(streamerField);
        streamerField.setOnKeyPressed(event ->
        {
            if (event.getCode().equals(KeyCode.ENTER))
            { confirmButton.fire(); }
        });

        styleButton(confirmButton);
        confirmButton.setOnAction(e ->
        {
            String channel = streamerField.getText();

            if (connectedToChannel)
            { disconnectFromChannel(); }

            if (!channel.substring(0, 1).contains("#"))
            { channel = "#" + channel; }

            channel = channel.toLowerCase();

            log("Connecting to " + channel);
            sendMessage("JOIN " + channel);

            session.setChannel(channel);
            displayMessage("Joining channel " + Session.getChannel() + "...");
            cancelButton.fire();
        });

        styleButton(cancelButton);
        cancelButton.setOnAction(e ->
        {
            userListPane.setContent(userList);
            messagePane.setVvalue(1.0);
            messagePane.vvalueProperty().bind(messageHolder.heightProperty());
            messagePane.setContent(messageHolder);
            messageField.setDisable(false);
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
        {
            if (btn instanceof Button)
            {
                VBox.setMargin(btn, new Insets(7, 0, 0, 0));
                ((Button) btn).minWidthProperty().bind(controller.minWidthProperty());
            }
        }

        contentHolder.prefHeightProperty().bind(messagePane.heightProperty());
        contentHolder.prefWidthProperty().bind(messagePane.widthProperty());
        userListPane.setContent(controller);
        messagePane.vvalueProperty().unbind();
        messagePane.setVvalue(0.0);
        messagePane.setContent(contentHolder);
        messageField.setDisable(true);
        title.requestFocus();
    }

    // TODO: Implement Better
    private void askForCredentials()
    {
        Stage secondaryStage = new Stage();
        BorderPane contentAlignmentPane = new BorderPane();
        VBox contentHolder = new VBox();

        contentAlignmentPane.setStyle("-fx-background-color: " + backgroundColor + ";");

        Label title = new Label("Please enter credentials");
        Button confirmButton = new Button("Confirm");
        TextField userNameTextField = new TextField(),
                oauthTextField = new TextField();
        userNameTextField.setPromptText("Username");
        oauthTextField.setPromptText("OAUTH KEY");

        contentHolder.setSpacing(20.0);
        contentHolder.setAlignment(Pos.CENTER);

        contentAlignmentPane.setCenter(contentHolder);
        contentAlignmentPane.setTop(title);
        contentHolder.getChildren().addAll(userNameTextField, oauthTextField, confirmButton);

        confirmButton.setOnAction(e ->
        {
            try
            {
                client = new Client(oauthTextField.getText(), userNameTextField.getText());
            } catch (IllegalOauthKey y)
            {
                log("Bad OAUTH key entered");
                title.setText("Bad OAUTH key");
                return;
            }

            if (client.isReady())
            {
                secondaryStage.close();
                credentialsAvailable = true;
            } else
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

        styleUILabel(title);
        styleTextField(userNameTextField);
        styleTextField(oauthTextField);
        styleButton(confirmButton);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(10));
        BorderPane.setMargin(contentHolder, new Insets(0, 30, 0, 30));

        Scene root = new Scene(contentAlignmentPane, 400, 300);
        secondaryStage.setScene(root);
        secondaryStage.setTitle("Enter credentials");
        secondaryStage.setOnShown(e -> title.requestFocus());
        secondaryStage.setOnCloseRequest(e ->
        {
            if (client == null || !client.isReady())
            {
                log("Credential window exit w/o client being initialized!");
                System.exit(1);
            }
        });
        secondaryStage.initStyle(StageStyle.UTILITY);
        secondaryStage.showAndWait();
    }

    private void showUISettingsWindow()
    {
        //TODO: Move label strings to UISettings and generate most of this based on each setting's data type
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
        Button closeButton = new Button("Back"),
                applyButton = new Button("Apply"),
                resetButton = new Button("Reset To Defaults"),
                shareUISettingsButton = new Button("Share Presets"),
                importUISettingsButton = new Button("Import Presets"),
                colorPickerButton = new Button("Color Picker");
        Label messageFontLabel = new Label("Message Font Size"),
                uiFontLabel = new Label("UI Font Size"),
                uiMessagePrefixLabel = new Label("Message Prefix"),
                textColorLabel = new Label("Text Color"),
                backgroundColorLabel = new Label("Background Color"),
                highlightColorLabel = new Label("HighLight Color"),
                uiAccentColorLabel = new Label("UI Accent Color"),
                highlightTextColorLabel = new Label("Highlight Text Color"),
                actionColorLabel = new Label("Action Text Color"),
                windowWidthLabel = new Label("Window Width"),
                windowHeightLabel = new Label("Window height");
        TextField messageFontInput = new TextField(String.valueOf((int) uiSettings.getMessageFontSize())),
                uiFontInput = new TextField(String.valueOf((int) uiSettings.getUiFont())),
                uiMessagePrefixInput = new TextField(uiSettings.getMessagePrefix()),
                textColorInput = new TextField(textFill),
                backgroundColorInput = new TextField(backgroundColor),
                highlighColorInput = new TextField(highlightColor),
                uiAccentColorInput = new TextField(uiAccentColor),
                highlightTextColorInput = new TextField(highlightTextColor),
                actionColorInput = new TextField(actionColor),
                windowWidthInput = new TextField(String.valueOf((int) root.getWidth())),
                windowHeightInput = new TextField(String.valueOf((int) root.getHeight()));
        Circle textColorCircle = new Circle(20.0),
                backgroundColorCircle = new Circle(20.0),
                highlighColorCircle = new Circle(20.0),
                uiAccentColorCircle = new Circle(20.0),
                highlightTextColorCircle = new Circle(20.0),
                actionColorCircle = new Circle(20.0);

        styleCircle(textColorCircle, textFill);
        styleCircle(backgroundColorCircle, backgroundColor);
        styleCircle(highlighColorCircle, highlightColor);
        styleCircle(uiAccentColorCircle, uiAccentColor);
        styleCircle(highlightTextColorCircle, highlightTextColor);
        styleCircle(actionColorCircle, actionColor);

        styleTextField(messageFontInput, 2.85);
        styleTextField(uiFontInput, 2.85);
        styleTextField(uiMessagePrefixInput, 6.1);
        styleTextField(textColorInput, 6.1);
        styleTextField(backgroundColorInput, 6.1);
        styleTextField(highlighColorInput, 6.1);
        styleTextField(uiAccentColorInput, 6.1);
        styleTextField(highlightTextColorInput, 6.1);
        styleTextField(actionColorInput, 6.1);
        styleTextField(windowWidthInput, 6.1);
        styleTextField(windowHeightInput, 6.1);

        styleUILabel(messageFontLabel);
        styleUILabel(uiFontLabel);
        styleUILabel(uiMessagePrefixLabel);
        styleUILabel(textColorLabel);
        styleUILabel(backgroundColorLabel);
        styleUILabel(highlightColorLabel);
        styleUILabel(uiAccentColorLabel);
        styleUILabel(highlightTextColorLabel);
        styleUILabel(actionColorLabel);
        styleUILabel(windowWidthLabel);
        styleUILabel(windowHeightLabel);

        styleUILabel(title);

        styleButton(closeButton);
        closeButton.setOnAction(e ->
        {
            messagePane.setVvalue(1.0); // scroll to the bottom
            messagePane.vvalueProperty().bind(messageHolder.heightProperty());
            messagePane.setContent(messageHolder);
            userListPane.setContent(userList);
            messageField.setDisable(false);
        });

        styleButton(applyButton);
        applyButton.setOnAction(e ->
        {
            uiSettings.setMessageFontSize(Double.valueOf(messageFontInput.getText()));
            uiSettings.setUiFont(Double.valueOf(uiFontInput.getText()));
            uiSettings.setMessagePrefix(uiMessagePrefixInput.getText());
            uiSettings.setTextFill(textColorInput.getText());
            uiSettings.setBackgroundColor(backgroundColorInput.getText());
            uiSettings.setHighlightColor(highlighColorInput.getText());
            uiSettings.setUIAccentColor(uiAccentColorInput.getText());
            uiSettings.setHighlightTextColor(highlightTextColorInput.getText());
            uiSettings.setActionColor(actionColorInput.getText());
            uiSettings.setWindowWidth(Integer.valueOf(windowWidthInput.getText()));
            uiSettings.setWindowHeight(Integer.valueOf(windowHeightInput.getText()));

            log("recording ui setting data");
            writeUISettingsToFile(uiSettings, uiSettingsFile);
        });

        styleButton(resetButton);
        resetButton.setOnAction(e ->
        {
            UISettings newSettings = new UISettings();

            writeUISettingsToFile(newSettings, uiSettingsFile);

            messageFontInput.setText(String.valueOf((int) newSettings.getMessageFontSize()));
            uiFontInput.setText(String.valueOf((int) newSettings.getUiFont()));
            uiMessagePrefixInput.setText(newSettings.getMessagePrefix());
            textColorInput.setText(newSettings.getTextFill());
            backgroundColorInput.setText(newSettings.getBackgroundColor());
            highlighColorInput.setText(newSettings.getHighlightColor());
            uiAccentColorInput.setText(newSettings.getUIAccentColor());
            highlightTextColorInput.setText(newSettings.getHighlightTextColor());
            actionColorInput.setText(newSettings.getActionColor());
            windowWidthInput.setText(String.valueOf((int) newSettings.getWindowWidth()));
            windowHeightInput.setText(String.valueOf((int) newSettings.getWindowHeight()));
        });

        styleButton(shareUISettingsButton);
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
                newSettings.setMessagePrefix(uiMessagePrefixInput.getText());
                newSettings.setTextFill(textColorInput.getText());
                newSettings.setBackgroundColor(backgroundColorInput.getText());
                newSettings.setHighlightColor(highlighColorInput.getText());
                newSettings.setUIAccentColor(uiAccentColorInput.getText());
                newSettings.setHighlightTextColor(highlightTextColorInput.getText());
                newSettings.setActionColor(actionColorInput.getText());
                newSettings.setWindowWidth(Integer.valueOf(windowWidthInput.getText()));
                newSettings.setWindowHeight(Integer.valueOf(windowHeightInput.getText()));

                writeUISettingsToFile(newSettings, saveToFile);
            }
        });

        styleButton(importUISettingsButton);
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
                    ObjectInputStream is = new ObjectInputStream(
                            new FileInputStream(importedObjectFile));
                    newSettings = (UISettings) is.readObject();
                    is.close();
                } catch (IOException | ClassNotFoundException y)
                {
                    log(y.getMessage());
                }
                if (newSettings != null)
                {
                    uiSettings.setMessageFontSize(newSettings.getMessageFontSize());
                    uiSettings.setUiFont(newSettings.getUiFont());
                    uiSettings.setMessagePrefix(newSettings.getMessagePrefix());
                    uiSettings.setTextFill(newSettings.getTextFill());
                    uiSettings.setBackgroundColor(newSettings.getBackgroundColor());
                    uiSettings.setHighlightColor(newSettings.getHighlightColor());
                    uiSettings.setUIAccentColor(newSettings.getUIAccentColor());
                    uiSettings.setHighlightTextColor(newSettings.getHighlightTextColor());
                    uiSettings.setActionColor(newSettings.getActionColor());
                    uiSettings.setWindowWidth(newSettings.getWindowWidth());
                    uiSettings.setWindowHeight(newSettings.getWindowHeight());

                    messageFontInput.setText(String.valueOf((int) uiSettings.getMessageFontSize()));
                    uiFontInput.setText(String.valueOf((int) uiSettings.getUiFont()));
                    uiMessagePrefixInput.setText(uiSettings.getMessagePrefix());
                    textColorInput.setText(uiSettings.getTextFill());
                    backgroundColorInput.setText(uiSettings.getBackgroundColor());
                    highlighColorInput.setText(uiSettings.getHighlightColor());
                    uiAccentColorInput.setText(uiSettings.getUIAccentColor());
                    highlightTextColorInput.setText(uiSettings.getHighlightTextColor());
                    actionColorInput.setText(uiSettings.getActionColor());
                    windowWidthInput.setText(String.valueOf((int) uiSettings.getWindowWidth()));
                    windowHeightInput.setText(String.valueOf((int) uiSettings.getWindowHeight()));
                }
            }
        });

        styleButton(colorPickerButton);
        colorPickerButton.setOnAction(e ->
                BareBonesBrowserLaunch
                        .openURL("https://duckduckgo.com/?q=color+picker&ia=answer")
        );

        messageFontLabel.setAlignment(Pos.CENTER_RIGHT);
        uiFontLabel.setAlignment(Pos.CENTER_RIGHT);
        uiMessagePrefixInput.setAlignment(Pos.CENTER_LEFT);

        contentHolder.setVgap(7.0);
        contentHolder.setHgap(15.0);

        int vPos = 0;
        contentHolder.add(messageFontLabel, 0, vPos);
        contentHolder.add(messageFontInput, 1, vPos);
        vPos++;

        contentHolder.add(uiFontLabel, 0, vPos);
        contentHolder.add(uiFontInput, 1, vPos);
        vPos++;

        contentHolder.add(uiMessagePrefixLabel, 0, vPos);
        contentHolder.add(uiMessagePrefixInput, 1, vPos);
        vPos++;

        contentHolder.add(textColorLabel, 0, vPos);
        contentHolder.add(textColorInput, 1, vPos);
        contentHolder.add(textColorCircle, 2, vPos);
        vPos++;

        contentHolder.add(backgroundColorLabel, 0, vPos);
        contentHolder.add(backgroundColorInput, 1, vPos);
        contentHolder.add(backgroundColorCircle, 2, vPos);
        vPos++;

        contentHolder.add(highlightColorLabel, 0, vPos);
        contentHolder.add(highlighColorInput, 1, vPos);
        contentHolder.add(highlighColorCircle, 2, vPos);
        vPos++;

        contentHolder.add(uiAccentColorLabel, 0, vPos);
        contentHolder.add(uiAccentColorInput, 1, vPos);
        contentHolder.add(uiAccentColorCircle, 2, vPos);
        vPos++;

        contentHolder.add(highlightTextColorLabel, 0, vPos);
        contentHolder.add(highlightTextColorInput, 1, vPos);
        contentHolder.add(highlightTextColorCircle, 2, vPos);
        vPos++;

        contentHolder.add(actionColorLabel, 0, vPos);
        contentHolder.add(actionColorInput, 1, vPos);
        contentHolder.add(actionColorCircle, 2, vPos);
        vPos++;

        contentHolder.add(windowWidthLabel, 0, vPos);
        contentHolder.add(windowWidthInput, 1, vPos);
        vPos++;

        contentHolder.add(windowHeightLabel, 0, vPos);
        contentHolder.add(windowHeightInput, 1, vPos);
        vPos++;

        controller.getChildren().addAll(closeButton, applyButton, resetButton,
                shareUISettingsButton, importUISettingsButton, colorPickerButton);

        controller.setAlignment(Pos.TOP_RIGHT);
        controller.minWidthProperty().bind(userListPane.widthProperty());
        contentHolder.minWidthProperty().bind(messagePane.widthProperty());

        // Fill the available space with the button
        for (Node btn : controller.getChildren())
        {
            if (btn instanceof Button)
            {
                VBox.setMargin(btn, new Insets(7, 0, 0, 0));
                ((Button) btn).prefWidthProperty().bind(controller.minWidthProperty());
            }
        }


        messagePane.setContent(dummy);
        userListPane.setContent(controller);
        messageField.setDisable(true);

        ChangeListener<String> fontInputChangeListener = ((obs, oldVal, newVal) ->
        {
            if (newVal.isEmpty())
                ((StringProperty) obs).setValue(newVal);

            else if (newVal.matches("\\d+") && Integer.parseInt(newVal) <= 35)
                ((StringProperty) obs).setValue(newVal);

            else
                ((StringProperty) obs).setValue(oldVal);
        });

        messageFontInput.textProperty().addListener(fontInputChangeListener);
        uiFontInput.textProperty().addListener(fontInputChangeListener);

        ChangeListener<String> intInputChangeListener = ((obs, oldVal, newVal) ->
        {
            if (newVal.isEmpty())
                ((StringProperty) obs).setValue(newVal);

            else if (newVal.matches("\\d+") && Integer.parseInt(newVal) > 0)
                ((StringProperty) obs).setValue(newVal);

            else
                ((StringProperty) obs).setValue(oldVal);
        });

        windowWidthInput.textProperty().addListener(intInputChangeListener);
        windowHeightInput.textProperty().addListener(intInputChangeListener);

        // TODO: Simplify?
        textColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty) obs).setValue(newVal);
                if (newVal.length() == 7)
                    styleCircle(textColorCircle, newVal);
            } else
                ((StringProperty) obs).setValue(oldVal);
        });
        backgroundColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty) obs).setValue(newVal);
                if (newVal.length() == 7)
                    styleCircle(backgroundColorCircle, newVal);
            } else
                ((StringProperty) obs).setValue(oldVal);
        });
        highlighColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty) obs).setValue(newVal);
                if (newVal.length() == 7)
                    styleCircle(highlighColorCircle, newVal);
            } else
                ((StringProperty) obs).setValue(oldVal);
        });
        uiAccentColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty) obs).setValue(newVal);
                if (newVal.length() == 7)
                    styleCircle(uiAccentColorCircle, newVal);
            } else
                ((StringProperty) obs).setValue(oldVal);
        });
        highlightTextColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty) obs).setValue(newVal);
                if (newVal.length() == 7)
                    styleCircle(highlightTextColorCircle, newVal);
            } else
                ((StringProperty) obs).setValue(oldVal);
        });
        actionColorInput.textProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal.matches("#[a-fA-F0-9]{0,6}"))
            {
                ((StringProperty) obs).setValue(newVal);
                if (newVal.length() == 7)
                    styleCircle(actionColorCircle, newVal);
            } else
                ((StringProperty) obs).setValue(oldVal);
        });
        // TODO: END
    }

    private void sendMessage(String message)
    {
        socketRunner.sendMessage(message.trim());
    }

    private void disconnectFromChannel()
    {
        if (connectedToChannel)
        {
            log("Disconnecting from " + Session.getChannel());
            sendMessage("PART " + Session.getChannel());
            displayMessage("Leaving channel " + Session.getChannel());
            WildChat.title.setValue("WildChat - " + VERSION);
            session = new Session();
            userList.removeAllUsers();
            hasUserState = false;
            connectedToChannel = false;
        } else
        {
            displayMessage("You are not connected to a channel!");
        }
    }
}
