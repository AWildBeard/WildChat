import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

public class UserList extends VBox
{
    public UserList() {}

    public void addUser(String userName, ArrayList<Image> badges)
    {
        if (indexOfUsersHBox(userName) != null)
        {
            addBadgesToUser(userName, badges);
            return;
        }

        HBox userContainer = new HBox();
        Label usernameLabel = new Label(userName);
        userContainer.setSpacing(3.0);

        if (badges != null)
            for (Image img : badges)
                userContainer.getChildren().add(new ImageView(img));

        userContainer.getChildren().add(usernameLabel);

        this.getChildren().add(userContainer);
    }

    public void addUser(String userName)
    {
        HBox userContainter = new HBox();
        Label userNameLabel = new Label(userName);

        userContainter.getChildren().add(userNameLabel);
        userContainter.setSpacing(3.0);

        this.getChildren().add(userContainter);
    }

    public Integer indexOfUsersHBox(String userToSearchFor)
    {
        for (Node hbox : this.getChildren())
            if (hbox instanceof HBox)
                for (Node node : ((HBox) hbox).getChildren())
                    if (node instanceof Label)
                        if (((Label) node).getText().equals(userToSearchFor))
                            return this.getChildren().indexOf(hbox);

        return null;
    }

    public void addBadgesToUser(String userToSearchFor, ArrayList<Image> badges)
    {
        Integer indexOfUsersHBox;
        Label userName = null;
        HBox alreadyLoadedHBox;
        HBox newHBox = new HBox();
        newHBox.setSpacing(3.0);

        // find the users hbox and test if the user exists
        if ((indexOfUsersHBox = indexOfUsersHBox(userToSearchFor)) != null)
        {
            alreadyLoadedHBox = (HBox) this.getChildren().get(indexOfUsersHBox);

            // Reuse the users name
            for (Node node : alreadyLoadedHBox.getChildren())
            {
                if (node instanceof Label)
                {
                    userName = (Label) node;
                }
            }

            for (Image img : badges)
                newHBox.getChildren().add(new ImageView(img));

            if (userName == null)
                throw new IllegalArgumentException("Failed to find username despite finding it earlier");

            newHBox.getChildren().add(userName);

            this.getChildren().remove(indexOfUsersHBox.intValue());

            this.getChildren().add(indexOfUsersHBox, newHBox);
        }
    }

    public void removeUser(String userToSearchFor)
    {
        if (indexOfUsersHBox(userToSearchFor) != null)
            this.getChildren().remove(indexOfUsersHBox(userToSearchFor).intValue());
    }
}
