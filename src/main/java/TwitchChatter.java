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

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/*
 * Description: Main GUI for TwitchChatter
 */

public class TwitchChatter extends Application
{

    // Containers
    private Stage primaryStage;
    private Scene root;
    /*
     * 2 rows. Top row is the menu bar. Bottom row is the
     * "base content"
     */
    private VBox baseNode = new VBox();
    /*
     * Hold the message typing field and the message viewing field
     */
    private VBox messageWriteView = new VBox();

    private AnchorPane anchorPane = new AnchorPane();

    // Content Holders
    private MenuBar menuBar = new MenuBar();
    private ScrollPane messageViewer = new ScrollPane();
    private TextField messageInputField = new TextField();
    private ListView<String> userList = new ListView<>();

    public void start(Stage primaryStage)
    {
        this.primaryStage = primaryStage;

        initUI();

        this.primaryStage.setScene(root);
        this.primaryStage.show();
    }

    private void initUI()
    {
        addChildren();
        setAlignment();
    }

    private void addChildren()
    {
        baseNode.getChildren().addAll(menuBar, anchorPane);
        anchorPane.getChildren().addAll(messageWriteView, userList);
        messageWriteView.getChildren().addAll(messageViewer, messageInputField);
        menuBar.getMenus().add(new Menu("PlaceHolder"));
        AnchorPane.setTopAnchor(messageWriteView, 0.0);
        AnchorPane.setLeftAnchor(messageWriteView, 0.0);
        AnchorPane.setBottomAnchor(messageWriteView, 0.0);
        AnchorPane.setBottomAnchor(userList, 5.0);
        anchorPane.setStyle("-fx-background-color: green");

        root = new Scene(baseNode, 600, 400);
    }

    private void setAlignment()
    {

    }

}
