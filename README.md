# LambdaTest Java SDK


Run the following command to install the dependencies of the project mentioned in `pom.xml`:

```sh
mvn clean install
```

Set the required environment variables:

```sh
export PROJECT_TOKEN=<add-your-project-token>
export LT_ACCESS_KEY=<add-your-access-key>
export LT_USERNAME=<add-your-username>
```

## Usage

###  Using the App Screenshot Function

#### 1. Create an Object of `SmartUIAppSnapshot`

```java
import java.util.HashMap;
import java.util.Map;

public class SmartUITest {
    public static void main(String[] args) {
        Map<String, String> screenshotConfig = new HashMap<>();
        screenshotConfig.put("deviceName", "Google Pixel 9");
        screenshotConfig.put("platform", "Android 15");

        SmartUIAppSnapshot smartUIAppSnapshot = new SmartUIAppSnapshot();
        
        try {
            smartUIAppSnapshot.start(); // Start authentication and create the build
            smartUIAppSnapshot.smartuiAppSnapshot(driver, "screenshot1", screenshotConfig); // Capture screenshot
        } catch (Exception e) {
            System.err.println("Error capturing SmartUI snapshot: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                smartUIAppSnapshot.stop(); // Ensure stop is always called
            } catch (Exception ex) {
                System.err.println("Error stopping SmartUI session: " + ex.getMessage());
            }
        }
    }
}
```


### Using the Web Screenshot Function

#### 1. Install the `smartui-cli` Dependencies

```sh
npm i @lambdatest/smartui-cli
```

#### 2. Create and Configure SmartUI Config

```sh
npx smartui config:create smartui-web.json
```
```java
import io.github.lambdatest.SmartUISnapshot;

public class SmartUISDK {

    private RemoteWebDriver driver;

    @Test
    public void basicTest() throws Exception {
        driver.get("https://www.lambdatest.com/support/docs/smartui-selenium-java-sdk");
        SmartUISnapshot.smartuiSnapshot(driver, "visual-regression-testing");
    }

}
```

#### 3. Use Local Hub

```sh
npx smartui exec -- mvn test -D suite=sdk-local.xml
```

#### 4. Use LambdaTest Cloud Hub

```sh
npx smartui exec -- mvn test -D suite=sdk-cloud.xml
```

The tests can be executed in the terminal using the following command:

```sh
mvn test -D suite=smartui.xml
```
