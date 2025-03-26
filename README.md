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
SmartUIAppSnapshot smartUIAppSnapshot = new SmartUIAppSnapshot();
```

#### 2. Add Screenshot Config for Taking a Screenshot

```java
Map<String, String> screenshotConfig = new HashMap<>();
screenshotConfig.put("deviceName", "Google Pixel 9");
screenshotConfig.put("platform", "Android 15");

try {
    smartUIAppSnapshot.start(); // Starts the auth process and creates the build
    smartUIAppSnapshot.smartuiAppSnapshot(driver, "screenshot1", screenshotConfig); // Takes a screenshot and uploads to SmartUI server
    smartUIAppSnapshot.stop();
} catch (Exception e) {
    e.printStackTrace();
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

#### 3. Use Local Hub

```sh
npx smartui exec -- mvn test -D suite=sdk-local.xml
```

#### 4. Use LambdaTest Cloud Hub

```sh
npx smartui exec -- mvn test -D suite=sdk-cloud.xml
```

Your test results will be displayed on the test console (or command-line interface if you are using terminal/cmd) and on the SmartUI dashboard.

## Executing SmartUI Test on LambdaTest Hub Without `smartui-cli`

The tests can be executed in the terminal using the following command:

```sh
mvn test -D suite=smartui.xml
```
