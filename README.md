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

###  To Take Smartui App Snapshot
`import io.github.lambdatest.SmartUIAppSnapshot;`
#### 1. Create an Object of `SmartUIAppSnapshot`

```java
        SmartUIAppSnapshot smartUIAppSnapshot = new SmartUIAppSnapshot();
        Map<String, String> screenshotConfig = new HashMap<>();
        screenshotConfig.put("deviceName","Google pixel 9");
        screenshotConfig.put("platform","Android 15");

        smartUIAppSnapshot.start(); 
        smartUIAppSnapshot.smartuiAppSnapshot(driver, "screenshot1", screenshotConfig);
        smartUIAppSnapshot.stop();
    
```


### To Take Smartui Snapshot

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
