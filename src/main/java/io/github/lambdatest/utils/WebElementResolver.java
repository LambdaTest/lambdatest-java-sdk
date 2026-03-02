package io.github.lambdatest.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Resolves WebElement objects in element, ignoreDOM, and selectDOM options to
 * CSS selectors before JSON serialization. This enables users to pass
 * WebElement objects directly instead of maintaining CSS selector strings
 * manually.
 */
public class WebElementResolver {

    private static final Logger log = LoggerUtil.createLogger("lambdatest-java-sdk");

    /**
     * Combined script that resolves a WebElement to a selector in a single
     * browser round-trip. Returns a Map with {type: "id"|"css", value: "..."}.
     */
    private static final String RESOLVE_SELECTOR_SCRIPT =
            "var el = arguments[0];" +
            "if (!el) return null;" +
            "if (el.id) return {type:'id', value: el.id};" +
            "var path = [];" +
            "while (el && el.nodeType === 1) {" +
            "  var selector = el.nodeName.toLowerCase();" +
            "  if (el.id) { path.unshift('#' + CSS.escape(el.id)); break; }" +
            "  var sib = el, nth = 1;" +
            "  while (sib = sib.previousElementSibling) {" +
            "    if (sib.nodeName === el.nodeName) nth++;" +
            "  }" +
            "  selector += ':nth-of-type(' + nth + ')';" +
            "  path.unshift(selector);" +
            "  el = el.parentNode;" +
            "}" +
            "var css = path.join(' > ');" +
            "if (css) return {type:'css', value: css};" +
            "return null;";

    /**
     * Scans the options map for element, ignoreDOM and selectDOM entries
     * containing WebElement objects, resolves them to CSS selectors, and
     * updates the map in-place.
     *
     * Supports:
     * - "element" option: WebElement for single element screenshot
     *   (resolves to {id: "..."} or {cssSelector: "..."})
     * - "ignoreDOM"/"selectDOM": "elements" key with List of WebElement
     *   objects, or mixed arrays. Note: the "elements" key is consumed
     *   (removed) during resolution.
     */
    @SuppressWarnings("unchecked")
    public static void resolveWebElements(JavascriptExecutor jsExecutor, Map<String, Object> options) {
        if (options == null)
            return;

        resolveElementOption(jsExecutor, options);
        resolveDOMOption(jsExecutor, options, "ignoreDOM");
        resolveDOMOption(jsExecutor, options, "selectDOM");
    }

    /**
     * Handles the "element" option for single element screenshots.
     * If the value is a WebElement, resolves it to {id: "..."} or
     * {cssSelector: "..."}. If it's a Map with string selectors (id, class,
     * cssSelector, xpath), validates the element exists on the page and logs
     * a warning if not found. Non-WebElement/non-Map values are left unchanged.
     */
    @SuppressWarnings("unchecked")
    private static void resolveElementOption(JavascriptExecutor jsExecutor, Map<String, Object> options) {
        Object elementOption = options.get("element");
        if (elementOption == null)
            return;

        if (elementOption instanceof WebElement) {
            Map<String, String> resolved = resolveToSelectorMap(jsExecutor, (WebElement) elementOption);
            if (resolved != null) {
                options.put("element", resolved);
            } else {
                log.warning("Failed to resolve WebElement for element screenshot");
            }
            return;
        }

        // Validate Map-based element selectors exist on the page
        if (elementOption instanceof Map) {
            Map<String, String> elementMap = (Map<String, String>) elementOption;
            String selector = null;

            if (elementMap.containsKey("id")) {
                selector = "#" + elementMap.get("id");
            } else if (elementMap.containsKey("class")) {
                selector = "." + elementMap.get("class");
            } else if (elementMap.containsKey("cssSelector")) {
                selector = elementMap.get("cssSelector");
            } else if (elementMap.containsKey("xpath")) {
                selector = elementMap.get("xpath");
            }

            if (selector != null) {
                validateElementExists(jsExecutor, selector, selector.equals(elementMap.get("xpath")));
            }
        }
    }

    private static final String VALIDATE_CSS_SCRIPT = "return document.querySelectorAll(arguments[0]).length > 0;";
    private static final String VALIDATE_XPATH_SCRIPT =
            "var result = document.evaluate(arguments[0], document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);" +
            "return result.snapshotLength > 0;";

    /**
     * Validates that an element matching the given selector exists on the page.
     * Logs a warning if not found but does not block execution.
     */
    private static void validateElementExists(JavascriptExecutor jsExecutor, String selector, boolean isXpath) {
        try {
            String script = isXpath ? VALIDATE_XPATH_SCRIPT : VALIDATE_CSS_SCRIPT;
            Boolean found = (Boolean) jsExecutor.executeScript(script, selector);
            if (found == null || !found) {
                log.warning("Element not found on page for selector: " + selector);
            }
        } catch (Exception e) {
            log.warning("Failed to validate element selector '" + selector + "': " + e.getMessage());
        }
    }

    /**
     * Resolves a single WebElement to a Map with either {id: "value"} or
     * {cssSelector: "value"} using a single browser round-trip.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> resolveToSelectorMap(JavascriptExecutor jsExecutor, WebElement element) {
        try {
            Map<String, Object> result = (Map<String, Object>) jsExecutor.executeScript(RESOLVE_SELECTOR_SCRIPT, element);
            if (result == null) return null;

            String type = (String) result.get("type");
            String value = (String) result.get("value");
            if (value == null || value.isEmpty()) return null;

            Map<String, String> selectorMap = new HashMap<>();
            if ("id".equals(type)) {
                selectorMap.put("id", value);
            } else {
                selectorMap.put("cssSelector", value);
            }
            return selectorMap;
        } catch (StaleElementReferenceException e) {
            log.warning("Skipping stale WebElement for element screenshot: element is no longer attached to the DOM");
        } catch (Exception e) {
            log.warning("Failed to resolve WebElement for element screenshot: " + e.getMessage());
        }
        return null;
    }

    /**
     * Resolves a WebElement and adds it to the appropriate key (id or
     * cssSelector) in the domMap using a single browser round-trip.
     */
    @SuppressWarnings("unchecked")
    private static void resolveAndAdd(JavascriptExecutor jsExecutor, WebElement element, Map<String, Object> domMap, String optionKey) {
        try {
            Map<String, Object> result = (Map<String, Object>) jsExecutor.executeScript(RESOLVE_SELECTOR_SCRIPT, element);
            if (result == null) return;

            String type = (String) result.get("type");
            String value = (String) result.get("value");
            if (value == null || value.isEmpty()) return;

            String key = "id".equals(type) ? "id" : "cssSelector";
            List<String> list = (List<String>) domMap.computeIfAbsent(key, k -> new ArrayList<String>());
            if (!list.contains(value)) {
                list.add(value);
            }
        } catch (StaleElementReferenceException e) {
            log.warning("Skipping stale WebElement in " + optionKey + ": element is no longer attached to the DOM");
        } catch (Exception e) {
            log.warning("Failed to resolve WebElement in " + optionKey + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void resolveDOMOption(JavascriptExecutor jsExecutor, Map<String, Object> options, String key) {
        Object domOption = options.get(key);
        if (domOption == null || !(domOption instanceof Map))
            return;

        Map<String, Object> domMap = (Map<String, Object>) domOption;

        // Handle "elements" key — a dedicated list of WebElement objects.
        // The "elements" key is consumed (removed) during resolution and
        // resolved entries are added to "id" or "cssSelector" keys.
        Object elementsObj = domMap.remove("elements");
        if (elementsObj instanceof List) {
            List<?> elements = (List<?>) elementsObj;
            for (Object item : elements) {
                if (item instanceof WebElement) {
                    resolveAndAdd(jsExecutor, (WebElement) item, domMap, key);
                }
            }
        }

        // Handle mixed arrays in existing keys (cssSelector, id, class, xpath)
        for (String selectorKey : new String[] { "cssSelector", "id", "class", "xpath" }) {
            Object value = domMap.get(selectorKey);
            if (!(value instanceof List))
                continue;

            List<?> list = (List<?>) value;
            boolean hasWebElements = false;
            for (Object item : list) {
                if (item instanceof WebElement) {
                    hasWebElements = true;
                    break;
                }
            }
            if (!hasWebElements)
                continue;

            List<String> strings = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String) {
                    strings.add((String) item);
                } else if (item instanceof WebElement) {
                    resolveAndAdd(jsExecutor, (WebElement) item, domMap, key);
                }
            }
            // Replace the mixed list with only the string values
            domMap.put(selectorKey, strings);
        }

        // Clean up empty lists
        domMap.entrySet().removeIf(entry -> entry.getValue() instanceof List && ((List<?>) entry.getValue()).isEmpty());
    }
}
