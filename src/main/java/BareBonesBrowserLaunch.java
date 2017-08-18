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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Arrays;

/**
 * @author Dem Pilafian
 * Modified for JavaFX by: AWildBeard
 * Origionally licensed under "Public Domain Software"
 * Source: http://centerkey.com/java/browser/
 */

public class BareBonesBrowserLaunch
{

    static final String[] browsers = {"x-www-browser", "google-chrome-stable",
            "google-chrome", "firefox", "opera", "epiphany", "konqueror", "conkeror",
            "midori", "kazehakase", "mozilla"};

    static final String errMsg = "Error attempting to launch web browser";

    public static void openURL(String url)
    {
        String osName = System.getProperty("os.name");
        try
        {
            if (osName.startsWith("Mac OS"))
            {
                Class.forName("com.apple.eio.FileManager").getDeclaredMethod(
                        "openURL", new Class[]{String.class}).invoke(null, url);
            } else if (osName.startsWith("Windows"))
            {
                Runtime.getRuntime().exec(
                        "rundll32 url.dll,FileProtocolHandler " + url);
            } else
            { //assume Unix or Linux
                String browser = null;
                for (String b : browsers)
                {
                    if (browser == null && Runtime.getRuntime().exec(new String[]
                            {"which", b}).getInputStream().read() != -1)
                        Runtime.getRuntime().exec(new String[]{browser = b, url});
                }
                if (browser == null)
                    throw new Exception(Arrays.toString(browsers));
            }
        } catch (Exception e)
        {
            Stage errorMessageStage = new Stage();
            BorderPane contentHolder = new BorderPane();
            Label errorMessage = new Label(errMsg + "\n" + e.toString());
            contentHolder.setCenter(errorMessage);
            BorderPane.setMargin(errorMessage, new Insets(20));
            BorderPane.setAlignment(errorMessage, Pos.CENTER);

            WildChat.styleUILabel(errorMessage);
            contentHolder.setStyle("-fx-background-color: " + WildChat.backgroundColor + ";");

            Scene root = new Scene(contentHolder, 200, 250);
            errorMessageStage.setScene(root);
            errorMessageStage.setTitle(errMsg);
            errorMessageStage.initStyle(StageStyle.UTILITY);
            errorMessageStage.show();
        }
    }
}
