# Element Bounding Box Detection

This document describes the new element bounding box detection functionality added to the LambdaTest Java SDK.

## Overview

The element bounding box detection feature allows you to capture the positions and dimensions of specific elements across a full-page screenshot. This is particularly useful for:

- Visual testing and element positioning validation
- Automated element interaction based on coordinates
- Debugging element layout issues
- Accessibility testing and element tracking

## Features

- **Multi-Platform Support**: Works with iOS, Android, Native apps, and WebView
- **Automatic Coordinate Conversion**: Converts viewport-relative to absolute page coordinates
- **Viewport Filtering**: Only detects elements completely within the current viewport
- **Comprehensive Logging**: Detailed logs for element detection and processing
- **Upload Pipeline**: Prepares element data for backend processing

## Usage

### SmartUIAppSnapshot Integration (Recommended)

```java
import io.github.lambdatest.SmartUIAppSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

// Create SmartUIAppSnapshot instance
SmartUIAppSnapshot smartUI = new SmartUIAppSnapshot();

// Start the session
Map<String, String> startOptions = new HashMap<>();
startOptions.put("projectToken", "your-project-token");
startOptions.put("buildName", "Element Detection Test");
smartUI.start(startOptions);

// Prepare options for snapshot with element detection
Map<String, String> snapshotOptions = new HashMap<>();
snapshotOptions.put("deviceName", "iPhone 12");
snapshotOptions.put("platform", "iOS");
snapshotOptions.put("fullPage", "true");
snapshotOptions.put("pageCount", "5");

// Add multiple selector types for element detection using ignoreBoxes structure
Map<String, List<String>> selectors = new HashMap<>();

// XPath selectors
selectors.put("xpath", Arrays.asList(
    "//button[@id='submit']",
    "//input[@type='text']",
    "//div[@class='header']"
));

// CSS class selectors
selectors.put("class", Arrays.asList(
    "header",
    "footer",
    "navigation"
));

// Accessibility ID selectors (for mobile apps)
selectors.put("accessibilityid", Arrays.asList(
    "submit-button",
    "search-input"
));

// Name selectors
selectors.put("name", Arrays.asList(
    "username",
    "password"
));

// ID selectors
selectors.put("id", Arrays.asList(
    "login-form",
    "search-box"
));

Map<String, Object> ignoreBoxesMap = new HashMap<>();
ignoreBoxesMap.put("selectors", selectors);



// Convert to JSON string for options
Gson gson = new Gson();
snapshotOptions.put("ignoreBoxes", gson.toJson(ignoreBoxesMap));

// Take snapshot with element detection
smartUI.smartuiAppSnapshot(driver, "test-screenshot", snapshotOptions);

// Alternative: Use FullPageScreenshotUtil directly for more control
FullPageScreenshotUtil screenshotUtil = new FullPageScreenshotUtil(driver, "/path/to/screenshots");

// Multi-selector approach (recommended)
Map<String, List<String>> selectors = new HashMap<>();
selectors.put("xpath", Arrays.asList("//button[@id='submit']"));
selectors.put("class", Arrays.asList("header", "footer"));
selectors.put("accessibilityid", Arrays.asList("submit-button"));
selectors.put("name", Arrays.asList("username", "password"));
selectors.put("id", Arrays.asList("login-form"));
selectors.put("css", Arrays.asList(".container .header"));

List<File> screenshots = screenshotUtil.captureFullPage(10, selectors);



// Stop the session
smartUI.stop();
```

### Direct FullPageScreenshotUtil Usage

#### Multi-Selector Approach
```java
import io.github.lambdatest.utils.FullPageScreenshotUtil;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// Define multiple selector types
Map<String, List<String>> selectors = new HashMap<>();
selectors.put("xpath", Arrays.asList("//button[@id='submit']", "//input[@type='text']"));
selectors.put("class", Arrays.asList("header", "footer", "navigation"));
selectors.put("accessibilityid", Arrays.asList("submit-button", "search-input"));
selectors.put("name", Arrays.asList("username", "password"));
selectors.put("id", Arrays.asList("login-form", "search-box"));
selectors.put("css", Arrays.asList(".container .header", "#main-content"));

// Create screenshot utility
FullPageScreenshotUtil screenshotUtil = new FullPageScreenshotUtil(driver, "/path/to/screenshots");

// Capture full page with multi-selector element detection
List<File> screenshots = screenshotUtil.captureFullPage(10, selectors);
```





## Architecture

### Core Classes

1. **ElementBoundingBox**: Data class representing element position and metadata
2. **ElementBoundingBoxUtil**: Utility class for element detection and processing
3. **FullPageScreenshotUtil**: Enhanced with element detection capabilities and multi-selector support

### API Methods

#### FullPageScreenshotUtil Methods
- **`captureFullPage(int pageCount)`**: Capture without element detection
- **`captureFullPage(int pageCount, List<String> xpaths)`**: XPath-only approach (deprecated)
- **`captureFullPage(int pageCount, Map<String, List<String>> selectors)`**: Multi-selector approach (recommended)

#### ElementBoundingBoxUtil Methods
- **`detectElements(List<String> xpaths, int chunkIndex)`**: XPath-only approach (deprecated)
- **`detectElements(Map<String, List<String>> selectors, int chunkIndex)`**: Multi-selector approach (recommended)

### Data Flow

```
XPath List → Platform Detection → Element Detection → Coordinate Conversion → 
Aggregation → Upload Preparation
```

## Element Detection Process

### 1. Platform Detection
- Automatically detects iOS/Android, Native/WebView
- Uses appropriate XPath evaluation methods
- Handles platform-specific element structures

### 2. Element Detection
- Evaluates selectors in current viewport
- Filters elements completely within viewport bounds
- Captures element location and dimensions

### 3. Coordinate Conversion
- Converts viewport-relative to absolute page coordinates
- Accounts for current scroll position
- Provides pixel-perfect positioning

## Output Format

### Element Data Structure

```json
{
  "timestamp": 1640995200000,
  "totalElements": 5,
  "platform": "ios_webview",
  "elements": [
    {
      "selectorType": "xpath",
      "selectorValue": "//button[@id='submit']",
      "selectorKey": "xpath://button[@id='submit']",
      "purpose": "select",
      "x": 100,
      "y": 250,
      "width": 80,
      "height": 40,
      "chunkIndex": 2,
      "timestamp": 1640995201000,
      "platform": "ios_webview"
    }
  ]
}
```

**Note**: The `purpose` field indicates whether the element is for:
- **`"ignore"`**: Elements that should be ignored during validation (e.g., ads, timestamps)
- **`"select"`**: Elements that should be selected for validation (e.g., buttons, forms)

### Log Output

```
INFO: Element detection enabled for 3 selectors
INFO: Element found: ElementBoundingBox{xpath='//button[@id='submit']', position=(100,250), size=(80,40), chunk=2, platform='ios_webview'}
INFO: Detected 2 elements in chunk 2
INFO: Element detection complete: 5 total elements
```

## Supported Selector Types

The element bounding box detection now supports multiple selector types for maximum flexibility:

### XPath Selectors
- **Usage**: `"xpath": ["//button[@id='submit']", "//input[@type='text']"]`
- **Best for**: Complex element selection, attribute-based selection
- **Platforms**: All platforms (iOS, Android, Web)

### CSS Class Selectors
- **Usage**: `"class": ["header", "footer", "navigation"]`
- **Best for**: Styling-based element selection
- **Platforms**: Web, WebView

### Accessibility ID Selectors
- **Usage**: `"accessibilityid": ["submit-button", "search-input"]`
- **Best for**: Mobile app testing, accessibility testing
- **Platforms**: iOS Native, Android Native

### Name Selectors
- **Usage**: `"name": ["username", "password", "email"]`
- **Best for**: Form elements, named elements
- **Platforms**: Web, WebView

### ID Selectors
- **Usage**: `"id": ["login-form", "search-box", "submit-btn"]`
- **Best for**: Unique element identification
- **Platforms**: Web, WebView

### CSS Selectors
- **Usage**: `"css": [".container .header", "#main-content", "button[type='submit']"]`
- **Best for**: Complex CSS-based selection
- **Platforms**: Web, WebView

## Platform Support

### iOS Native
- Uses iOS-specific XPath evaluation
- Handles native UI elements
- Supports accessibility attributes and accessibility IDs

### Android Native
- Uses Android-specific XPath evaluation
- Handles native UI elements
- Supports resource IDs, accessibility IDs, and accessibility attributes

### WebView (iOS/Android)
- Uses standard Selenium evaluation for all selector types
- Handles web content within native apps
- Supports all selector types (XPath, CSS, class, name, ID)

### Web Browser
- Uses standard Selenium evaluation for all selector types
- Handles web page elements
- Supports all selector types (XPath, CSS, class, name, ID)

## Platform-Specific Scrolling

The element bounding box detection uses platform-specific scrolling methods to ensure optimal performance and reliability across different environments.

### iOS Scrolling
- **Method**: Uses `mobile:dragFromToForDuration` Appium command
- **Parameters**:
  - `fromX`, `fromY`: Starting coordinates (center of screen)
  - `toX`, `toY`: Ending coordinates (calculated based on screen height)
  - `duration`: 5.0 seconds (slow, controlled scrolling)
- **Scroll Distance**: Calculated as 80% of screen height for optimal coverage
- **Wait Time**: 200ms between scrolls for stability
- **Advantages**: Precise control, works with all iOS versions, reliable element detection

### Android Scrolling
- **Method**: Uses `mobile:scrollGesture` Appium command
- **Parameters**:
  - `left`, `top`, `width`, `height`: Viewport dimensions
  - `direction`: "down" for vertical scrolling
  - `percent`: 0.8 (80% of screen height)
  - `speed`: 500ms (slow, controlled scrolling)
- **Scroll Distance**: 80% of screen height for optimal coverage
- **Wait Time**: 200ms between scrolls for stability
- **Advantages**: Native Android gesture, smooth scrolling, reliable detection

### Web Scrolling
- **Method**: Uses JavaScript `window.scrollBy()` function
- **Parameters**:
  - `x`: 0 (no horizontal scroll)
  - `y`: Calculated scroll distance (80% of viewport height)
- **Scroll Distance**: 80% of viewport height for optimal coverage
- **Wait Time**: 200ms between scrolls for stability
- **Advantages**: Standard web scrolling, works across all browsers, predictable behavior

### Scroll End Detection
- **Method**: Page source comparison
- **Process**: Compares current page source with previous page source
- **Logic**: If page source is identical, scrolling has reached the bottom
- **Wait Time**: 1 second after each scroll before checking
- **Fallback**: Assumes bottom reached if comparison fails

### Scroll Position Tracking
- **CSS Pixels**: All scroll positions tracked in CSS pixels for consistency
- **Cumulative Tracking**: Maintains total scroll distance across all chunks
- **Platform Agnostic**: Same tracking method for all platforms
- **Absolute Position**: Used for accurate element coordinate calculation

### Performance Optimization
- **Slow Scrolling**: All platforms use slow scroll speeds for reliable element detection
- **Minimal Wait Times**: 200ms between scrolls balances precision and performance
- **Consistent Coverage**: 80% scroll distance ensures no content is missed
- **Platform Detection**: Automatic detection ensures correct scroll method is used

## Configuration

### ignoreBoxes Options Format

The element detection uses the `ignoreBoxes` key with a nested map structure to support multiple selector types. The system now supports multiple formats for maximum flexibility:

1. **New Flexible Structure**: Users can send `ignoreBoxes` and/or `selectBoxes` directly
2. **Legacy Structure**: Users can send `ignore` and/or `select` keys  
3. **Legacy XPath Format**: Users can send `xpaths` list for backward compatibility

### Supported Input Formats

#### Format 1: Direct ignoreBoxes/selectBoxes (Recommended)
```java
Map<String, Object> mainMap = new HashMap<>();

// Send ignoreBoxes with various selector types
Map<String, List<String>> ignoreSelectors = new HashMap<>();
ignoreSelectors.put("xpath", Arrays.asList("//button[@id='submit']"));
ignoreSelectors.put("accessibilityid", Arrays.asList("submit-button"));
ignoreSelectors.put("class", Arrays.asList("header", "footer"));
mainMap.put("ignoreBoxes", ignoreSelectors);

// Send selectBoxes with various selector types
Map<String, List<String>> selectSelectors = new HashMap<>();
selectSelectors.put("xpath", Arrays.asList("//input[@type='text']"));
selectSelectors.put("id", Arrays.asList("login-form"));
mainMap.put("selectBoxes", selectSelectors);

// Convert to JSON
Gson gson = new Gson();
options.put("ignoreBoxes", gson.toJson(mainMap));
```

#### Format 2: Legacy ignore/select keys
```java
Map<String, Object> mainMap = new HashMap<>();

// Create selectors map
Map<String, List<String>> selectors = new HashMap<>();
selectors.put("xpath", Arrays.asList("//button[@id='submit']"));
selectors.put("accessibilityid", Arrays.asList("submit-button"));

// Use legacy keys
mainMap.put("ignore", selectors);  // or "select"

// Convert to JSON
Gson gson = new Gson();
options.put("ignoreBoxes", gson.toJson(mainMap));
```

#### Format 3: Legacy XPath-only (Backward Compatible)
```java
// Simple XPath list
List<String> xpaths = Arrays.asList(
    "//*[@text=\"Terms of Service | Privacy Policy\"]",
    "//*[@text=\"Sauce Labs Bolt T-Shirt\"]"
);

Map<String, Object> mainMap = new HashMap<>();
mainMap.put("xpaths", xpaths);

// Convert to JSON
Gson gson = new Gson();
options.put("ignoreBoxes", gson.toJson(mainMap));
```

#### New Flexible Structure (Recommended)
```java
// Create selectors with multiple selector types
Map<String, List<String>> ignoreSelectors = new HashMap<>();

// XPath selectors
ignoreSelectors.put("xpath", Arrays.asList(
    "//button[@id='submit']",
    "//input[@type='text']",
    "//div[@class='header']"
));

// CSS class selectors
ignoreSelectors.put("class", Arrays.asList(
    "header",
    "footer",
    "navigation"
));

// Accessibility ID selectors (for mobile apps)
ignoreSelectors.put("accessibilityid", Arrays.asList(
    "submit-button",
    "search-input"
));

// Name selectors
ignoreSelectors.put("name", Arrays.asList(
    "username",
    "password"
));

// ID selectors
ignoreSelectors.put("id", Arrays.asList(
    "login-form",
    "search-box"
));

// CSS selectors
ignoreSelectors.put("css", Arrays.asList(
    ".container .header",
    "#main-content"
));

// Create the main structure - user can send either ignoreBoxes or selectBoxes
Map<String, Object> mainMap = new HashMap<>();

// Option 1: Send ignoreBoxes
mainMap.put("ignoreBoxes", ignoreSelectors);

// Option 2: Send selectBoxes (or both)
Map<String, List<String>> selectSelectors = new HashMap<>();
selectSelectors.put("xpath", Arrays.asList("//button[@id='submit']"));
selectSelectors.put("class", Arrays.asList("important-button"));
mainMap.put("selectBoxes", selectSelectors);

// Convert to JSON string for options
Gson gson = new Gson();
snapshotOptions.put("ignoreBoxes", gson.toJson(mainMap));
```

#### Legacy Structure (Still Supported)
```java
// Create the ignoreBoxes structure with multiple selector types
Map<String, List<String>> selectors = new HashMap<>();

// XPath selectors
selectors.put("xpath", Arrays.asList(
    "//button[@id='submit']",
    "//input[@type='text']",
    "//div[@class='header']"
));

// CSS class selectors
selectors.put("class", Arrays.asList(
    "header",
    "footer",
    "navigation"
));

// Accessibility ID selectors (for mobile apps)
selectors.put("accessibilityid", Arrays.asList(
    "submit-button",
    "search-input"
));

// Name selectors
selectors.put("name", Arrays.asList(
    "username",
    "password"
));

// ID selectors
selectors.put("id", Arrays.asList(
    "login-form",
    "search-box"
));

// CSS selectors
selectors.put("css", Arrays.asList(
    ".container .header",
    "#main-content"
));

Map<String, Object> ignoreBoxesMap = new HashMap<>();
ignoreBoxesMap.put("ignore", selectors);  // or "select" depending on your needs

// Convert to JSON string for options
Gson gson = new Gson();
snapshotOptions.put("ignoreBoxes", gson.toJson(ignoreBoxesMap));
```

#### Legacy XPath-Only Format (Backward Compatible)
```java
// Simple XPath list format (automatically converted to new format)
List<String> xpaths = Arrays.asList(
    "//*[@text=\"Terms of Service | Privacy Policy\"]",
    "//*[@text=\"Sauce Labs Bolt T-Shirt\"]"
);

Map<String, Object> ignoreBoxesMap = new HashMap<>();
ignoreBoxesMap.put("xpaths", xpaths);

// Optional: specify purpose (defaults to "ignore" if not specified)
// ignoreBoxesMap.put("purpose", "select");  // Use "select" instead of "ignore"

Gson gson = new Gson();
Map<String, String> options = new HashMap<>();
options.put("ignoreBoxes", gson.toJson(ignoreBoxesMap));
options.put("fullPage", "true");
options.put("cropStatusBar", "false");
```



#### Future Extensibility
The structure is designed to support additional selector types in the future:
```json
{
  "xpath": ["//button[@id='submit']", "//input[@type='text']"],
  "classes": ["header", "footer", "navigation"],
  "accessibilityIds": ["submit-button", "search-input"],
  "resourceIds": ["com.example:id/button", "com.example:id/input"]
}
```

#### JSON Structure
The `ignoreBoxes` option expects a JSON string with the following structure:
```json
{
  "xpath": [
    "//button[@id='submit']",
    "//input[@type='text']",
    "//div[@class='header']"
  ]
}
```

### Viewport Filtering
- Only detects elements completely within viewport
- Prevents partial element detection
- Ensures accurate positioning data

## Error Handling

### Selector Failures
- Logs warning and continues with other selectors
- Does not fail the entire capture process
- Provides detailed error information

### Coordinate Errors
- Logs warning for invalid coordinates
- Skips elements with positioning issues
- Maintains process stability

### Platform Detection
- Defaults to "unknown" if detection fails
- Continues with standard XPath evaluation
- Logs detection issues

## Performance Considerations

### Synchronous Processing
- Element detection happens after each screenshot
- Minimal impact on capture performance
- Predictable timing and behavior

### Memory Management
- Clears temporary data after each chunk
- Efficient element detection algorithms
- Minimal memory footprint

### Future Enhancements
- Asynchronous processing for better performance
- Batch element detection
- Advanced caching strategies

## Integration with Backend

### Complete Flow from Start to End

1. **Input Configuration**
   ```java
   Map<String, Object> ignoreBoxesMap = new HashMap<>();
   ignoreBoxesMap.put("ignore", ignoreSelectors);
   ignoreBoxesMap.put("select", selectSelectors);
   
   Map<String, String> options = new HashMap<>();
   options.put("ignoreBoxes", gson.toJson(ignoreBoxesMap));
   ```

2. **Element Detection**
   - `SmartUIAppSnapshot` extracts both ignore and select selectors
   - `FullPageScreenshotUtil` captures screenshots with element detection
   - `ElementBoundingBoxUtil` detects elements with appropriate purpose ("ignore" or "select")

3. **Bounding Box Creation**
   - Ignore elements → `ignoreBoxes` payload
   - Select elements → `selectBoxes` payload
   - Both include coordinates: `left`, `top`, `right`, `bottom`

4. **Upload API**
   - `uploadSnapshotRequest.setIgnoreBoxes(ignoreBoxesJson)`
   - `uploadSnapshotRequest.setSelectBoxes(selectBoxesJson)`
   - Backend receives both payloads for processing

### Upload Pipeline
- Prepares structured data for backend processing
- Includes metadata and element information
- Compatible with existing upload mechanisms
- **NEW**: Sends both ignore and select element data

### Upload Payload Structure

The upload API now sends both `ignoreBoxes` and `selectBoxes` in the payload, allowing the backend to distinguish between elements that should be ignored versus those that should be selected for validation.

#### Upload Payload Example
```json
{
  "ignoreBoxes": {
    "boxes": [
      {
        "left": 100,
        "top": 200,
        "right": 180,
        "bottom": 240
      }
    ]
  },
  "selectBoxes": {
    "boxes": [
      {
        "left": 150,
        "top": 300,
        "right": 230,
        "bottom": 340
      }
    ]
  }
}
```

### Distinguishing Ignore vs Select Elements

When processing the detected elements later in your code, you can easily distinguish between ignore and select elements using the `purpose` field:

```java
// Process detected elements based on their purpose
for (ElementBoundingBox element : allDetectedElements) {
    if ("ignore".equals(element.getPurpose())) {
        // This element should be ignored during validation
        log.info("Ignoring element: " + element.getSelectorValue());
        // Add to ignore boxes for screenshot processing
        ignoreBoxes.add(createIgnoreBox(element));
        
    } else if ("select".equals(element.getPurpose())) {
        // This element should be validated
        log.info("Selecting element for validation: " + element.getSelectorValue());
        // Add to validation list
        validationElements.add(element);
    }
}

// Store elements separately for different purposes
List<ElementBoundingBox> ignoredElements = allDetectedElements.stream()
    .filter(e -> "ignore".equals(e.getPurpose()))
    .collect(Collectors.toList());

List<ElementBoundingBox> selectedElements = allDetectedElements.stream()
    .filter(e -> "select".equals(e.getPurpose()))
    .collect(Collectors.toList());

log.info("Found " + ignoredElements.size() + " ignore elements and " + selectedElements.size() + " select elements");
```

### Stitching Support
- Provides element data for server-side stitching
- Maintains element positions across chunks
- Enables full-page element mapping

## Best Practices

### Selector Selection
- Use specific, unique selectors
- Avoid overly broad selectors
- Consider element importance and frequency

### Performance Optimization
- Limit XPath list to essential elements
- Use efficient XPath expressions
- Monitor detection performance

### Error Handling
- Implement proper exception handling
- Monitor logs for detection issues
- Validate element data quality

## Troubleshooting

### Common Issues

1. **No Elements Detected**
   - Verify XPath syntax
   - Check element visibility
   - Confirm platform compatibility

2. **Coordinate Issues**
   - Verify scroll position calculation
   - Check viewport size detection
   - Validate platform detection

### Debug Information
- Enable detailed logging
- Monitor element detection process
- Review coordinate conversion logic

## Future Roadmap

- **Asynchronous Processing**: Improve performance with async element detection
- **Element State Tracking**: Track visibility and state changes
- **Performance Metrics**: Add timing and memory usage tracking
- **Enhanced Platform Support**: Additional platform-specific optimizations 

---

## Bounding Box Detection & Calculation Context

### 1. Coordinate System
- All calculations (element location, size, scroll position, viewport checks) are performed in CSS pixels.
- Device pixel ratio (DPR) is applied only once at the end to convert bounding box coordinates and dimensions from CSS pixels to device pixels for final output.

### 2. Bounding Box Creation
- For each detected element:
  - Location and size are obtained in CSS pixels.
  - Absolute position is calculated as:
    absoluteY = elementViewportY + cumulativeScrollY (all in CSS pixels)
  - Bounding box is created and stored in CSS pixels during all intermediate steps.

### Absolute Position Calculation
- The element's Y coordinate (`elementViewportY`) is relative to the current viewport (in CSS pixels).
- The current scroll position (`cumulativeScrollY`, in CSS pixels) is tracked as the user scrolls.
- The **absolute Y position** of the bounding box with respect to the top of the screen is calculated as:
  - `absoluteY = elementViewportY + cumulativeScrollY` (all in CSS pixels)
- At the end, this value is multiplied by the device pixel ratio (DPR) to convert to device pixels for upload:
  - `deviceAbsoluteY = absoluteY * DPR`
- This approach is consistent for both web and native apps, as scroll position is always tracked in CSS pixels.

### 3. Viewport Filtering
- Viewport size is obtained in CSS pixels.
- Element is considered in viewport if:
  - viewportRelativeY >= 0
  - viewportRelativeY + elementHeight <= viewportHeight
  - (and similarly for X axis)
- All viewport checks are done in CSS pixels.

### 4. Final Conversion
- At the end of detection, all bounding boxes are converted from CSS pixels to device pixels:
  - deviceX = cssX * DPR
  - deviceY = cssY * DPR
  - deviceWidth = cssWidth * DPR
  - deviceHeight = cssHeight * DPR
- This is done via a dedicated method:
  convertBoundingBoxesToDevicePixels(List<ElementBoundingBox> cssElements)

### 5. Platform Handling
- DPR is determined via:
  - JavaScript for web
  - Hardcoded map for iOS/iPad (using ios-resolution.com)
  - Capabilities or default for Android
- Scroll position is tracked in CSS pixels for all platforms.

### 6. Logging & Debugging
- Extensive logging is present for:
  - Element coordinates (CSS and device pixels)
  - Viewport checks
  - Conversion steps

---

This context ensures all bounding box logic is consistent, pixel-perfect, and platform-agnostic, with a single DPR conversion at the end for output. 