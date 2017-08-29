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

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

import static UISettings.ReadOnlyUISettings.getTextFill;
import static UISettings.ReadOnlyUISettings.getUiFont;
import static logUtils.Logger.log;

public class UserList extends VBox
{
    private ArrayList<String> users = new ArrayList<>();

    public UserList() {}

    public void addUser(String userName, ArrayList<Image> badges)
    {
        if (badges == null)
        {
            addUser(userName);
            return;
        }

        if (indexOfUsersHBox(userName) != null)
        {
            addBadgesToUser(userName, badges);
            return;
        }

        addUser(userName);
        addBadgesToUser(userName, badges);
    }

    public void addUser(String userName)
    {
        if (indexOfUsersHBox(userName) != null)
            return;

        HBox userContainter = new HBox();
        Label userNameLabel = new Label(userName);
        userNameLabel.setStyle("-fx-font-size: " + getUiFont() + ";" +
                "-fx-text-fill: " + getTextFill() + ";");

        userNameLabel.setCache(true);

        userContainter.getChildren().add(userNameLabel);
        userContainter.setSpacing(3.0);
        userContainter.setCache(true);

        users.add(userName);

        this.getChildren().add(userContainter);
    }

    public Integer indexOfUsersHBox(String userToSearchFor)
    {
        for (Node hbox : this.getChildren())
        {
            if (hbox instanceof HBox)
            {
                for (Node node : ((HBox) hbox).getChildren())
                {
                    if (node instanceof Label)
                    {
                        if (((Label) node).getText().equals(userToSearchFor))
                        { return this.getChildren().indexOf(hbox); }
                    }
                }
            }
        }

        return null;
    }

    public void addBadgesToUser(String userToSearchFor, ArrayList<Image> badges)
    {
        Integer indexOfUsersHBox;

        // find the users hbox and test if the user exists
        if ((indexOfUsersHBox = indexOfUsersHBox(userToSearchFor)) != null)
        {
            Label userName = null;
            HBox alreadyLoadedHBox;
            HBox newHBox = new HBox();
            newHBox.setAlignment(Pos.CENTER_LEFT);
            newHBox.setSpacing(WildChat.uiFont * 0.33);

            alreadyLoadedHBox = (HBox) this.getChildren().get(indexOfUsersHBox);

            // Reuse the users name
            for (Node node : alreadyLoadedHBox.getChildren())
                if (node instanceof Label)
                    userName = (Label) node;

            for (Image img : badges)
            {
                ImageView badge = new ImageView(img);
                badge.setCache(true);
                newHBox.getChildren().add(badge);
            }

            if (userName == null)
                throw new IllegalArgumentException("Failed to find username despite finding it earlier");

            if (! userName.isCache())
                userName.setCache(true);

            newHBox.getChildren().add(userName);

            this.getChildren().remove(indexOfUsersHBox.intValue());

            newHBox.setCache(true);
            this.getChildren().add(indexOfUsersHBox, newHBox);
        } else
            addUser(userToSearchFor, badges);
    }

    public void removeUser(String userToSearchFor)
    {
        Integer index;
        if ((index = indexOfUsersHBox(userToSearchFor)) != null)
            this.getChildren().remove(index.intValue());
    }

    public void removeAllUsers()
    {
        this.getChildren().clear();
    }

    public ArrayList<String> getUsers()
    {
        return users;
    }
}
