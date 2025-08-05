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
- **Deduplication**: Prevents duplicate element detection using position proximity
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

// Add XPaths for element detection using ignoreBoxes structure
List<String> xpaths = Arrays.asList(
    "//button[@id='submit']",
    "//input[@type='text']",
    "//div[@class='header']"
);

Map<String, Object> ignoreBoxesMap = new HashMap<>();
ignoreBoxesMap.put("xpaths", xpaths);

// Convert to JSON string for options
Gson gson = new Gson();
snapshotOptions.put("ignoreBoxes", gson.toJson(ignoreBoxesMap));

// Take snapshot with element detection
smartUI.smartuiAppSnapshot(driver, "test-screenshot", snapshotOptions);

// Stop the session
smartUI.stop();
```

### Direct FullPageScreenshotUtil Usage

```java
import io.github.lambdatest.utils.FullPageScreenshotUtil;
import java.util.Arrays;
import java.util.List;

// Define XPaths to detect
List<String> xpaths = Arrays.asList(
    "//button[@id='submit']",
    "//input[@type='text']",
    "//div[@class='header']"
);

// Create screenshot utility
FullPageScreenshotUtil screenshotUtil = new FullPageScreenshotUtil(driver, "/path/to/screenshots");

// Capture full page with element detection
List<File> screenshots = screenshotUtil.captureFullPage(10, xpaths);

// Capture without element detection (original behavior)
List<File> screenshots = screenshotUtil.captureFullPage(10);
```

## Architecture

### Core Classes

1. **ElementBoundingBox**: Data class representing element position and metadata
2. **ElementBoundingBoxUtil**: Utility class for element detection and processing
3. **FullPageScreenshotUtil**: Enhanced with element detection capabilities

### Data Flow

```
XPath List → Platform Detection → Element Detection → Coordinate Conversion → 
Deduplication → Aggregation → Upload Preparation
```

## Element Detection Process

### 1. Platform Detection
- Automatically detects iOS/Android, Native/WebView
- Uses appropriate XPath evaluation methods
- Handles platform-specific element structures

### 2. Element Detection
- Evaluates XPaths in current viewport
- Filters elements completely within viewport bounds
- Captures element location and dimensions

### 3. Coordinate Conversion
- Converts viewport-relative to absolute page coordinates
- Accounts for current scroll position
- Provides pixel-perfect positioning

### 4. Deduplication
- Uses position proximity (10-pixel threshold)
- Prevents duplicate element detection across chunks
- Maintains element uniqueness

## Output Format

### Element Data Structure

```json
{
  "timestamp": 1640995200000,
  "totalElements": 5,
  "platform": "ios_webview",
  "elements": [
    {
      "xpath": "//button[@id='submit']",
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

### Log Output

```
INFO: Element detection enabled for 3 XPaths
INFO: Element found: ElementBoundingBox{xpath='//button[@id='submit']', position=(100,250), size=(80,40), chunk=2, platform='ios_webview'}
INFO: Detected 2 elements in chunk 2
INFO: Deduplicating element: ElementBoundingBox{...} (too close to: ElementBoundingBox{...})
INFO: Deduplication complete: 8 -> 5 elements
INFO: Element detection complete: 8 total, 5 unique elements
```

## Platform Support

### iOS Native
- Uses iOS-specific XPath evaluation
- Handles native UI elements
- Supports accessibility attributes

### Android Native
- Uses Android-specific XPath evaluation
- Handles native UI elements
- Supports resource IDs and accessibility

### WebView (iOS/Android)
- Uses standard Selenium XPath evaluation
- Handles web content within native apps
- Supports CSS selectors and web elements

### Web Browser
- Uses standard Selenium XPath evaluation
- Handles web page elements
- Supports all web element types

## Configuration

### ignoreBoxes Options Format

The element detection uses the `ignoreBoxes` key with a nested map structure to support future extensibility:

#### Current Structure (XPath Support)
```java
// Create the ignoreBoxes structure
List<String> xpaths = Arrays.asList(
    "//button[@id='submit']",
    "//input[@type='text']",
    "//div[@class='header']"
);

Map<String, Object> ignoreBoxesMap = new HashMap<>();
ignoreBoxesMap.put("xpaths", xpaths);

// Convert to JSON string for options
Gson gson = new Gson();
snapshotOptions.put("ignoreBoxes", gson.toJson(ignoreBoxesMap));
```

#### Future Extensibility
The structure is designed to support additional selector types in the future:
```json
{
  "xpaths": ["//button[@id='submit']", "//input[@type='text']"],
  "classes": ["header", "footer", "navigation"],
  "accessibilityIds": ["submit-button", "search-input"],
  "resourceIds": ["com.example:id/button", "com.example:id/input"]
}
```

#### JSON Structure
The `ignoreBoxes` option expects a JSON string with the following structure:
```json
{
  "xpaths": [
    "//button[@id='submit']",
    "//input[@type='text']",
    "//div[@class='header']"
  ]
}
```

### Proximity Threshold
- Default: 10 pixels
- Configurable in `ElementBoundingBoxUtil.PROXIMITY_THRESHOLD`
- Used for deduplication logic

### Viewport Filtering
- Only detects elements completely within viewport
- Prevents partial element detection
- Ensures accurate positioning data

## Error Handling

### XPath Failures
- Logs warning and continues with other XPaths
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
- Efficient deduplication algorithms
- Minimal memory footprint

### Future Enhancements
- Asynchronous processing for better performance
- Batch element detection
- Advanced caching strategies

## Integration with Backend

### Upload Pipeline
- Prepares structured data for backend processing
- Includes metadata and element information
- Compatible with existing upload mechanisms

### Stitching Support
- Provides element data for server-side stitching
- Maintains element positions across chunks
- Enables full-page element mapping

## Best Practices

### XPath Selection
- Use specific, unique XPaths
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

2. **Duplicate Elements**
   - Adjust proximity threshold
   - Review XPath specificity
   - Check element positioning

3. **Coordinate Issues**
   - Verify scroll position calculation
   - Check viewport size detection
   - Validate platform detection

### Debug Information
- Enable detailed logging
- Monitor element detection process
- Review coordinate conversion logic

## Future Roadmap

- **Asynchronous Processing**: Improve performance with async element detection
- **Advanced Deduplication**: Content-based and visual similarity matching
- **Element State Tracking**: Track visibility and state changes
- **Performance Metrics**: Add timing and memory usage tracking
- **Enhanced Platform Support**: Additional platform-specific optimizations 