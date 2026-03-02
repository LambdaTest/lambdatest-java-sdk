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
 * Resolves WebElement objects in ignoreDOM/selectDOM options to CSS selectors
 * before JSON serialization. This enables users to pass WebElement objects
 * directly
 * instead of maintaining CSS selector strings manually.
 */
public class WebElementResolver {

    private static final Logger log = LoggerUtil.createLogger("lambdatest-java-sdk");

    private static final String CSS_SELECTOR_SCRIPT = "var el = arguments[0];" +
            "if (!el) return null;" +
            "if (el.id) return el.id;" +
            "var path = [];" +
            "while (el && el.nodeType === 1) {" +
            "  var selector = el.nodeName.toLowerCase();" +
            "  if (el.id) { path.unshift('#' + el.id); break; }" +
            "  var sib = el, nth = 1;" +
            "  while (sib = sib.previousElementSibling) {" +
            "    if (sib.nodeName === el.nodeName) nth++;" +
            "  }" +
            "  selector += ':nth-of-type(' + nth + ')';" +
            "  path.unshift(selector);" +
            "  el = el.parentNode;" +
            "}" +
            "return path.join(' > ');";

    private static final String ID_SCRIPT = "return arguments[0].id || null;";

    /**
     * Scans the options map for element, ignoreDOM and selectDOM entries containing
     * WebElement
     * objects, resolves them to CSS selectors, and updates the map in-place.
     *
     * Supports:
     * - "element" option: WebElement for single element screenshot (resolves to
     * {id: "..."} or {cssSelector: "..."})
     * - "ignoreDOM"/"selectDOM": "elements" key with List of WebElement objects, or
     * mixed arrays
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
     * If the value is a WebElement, resolves it to {id: "..."} or {cssSelector:
     * "..."}.
     * If it's a Map containing a WebElement value, resolves that value.
     */
    @SuppressWarnings("unchecked")
    private static void resolveElementOption(JavascriptExecutor jsExecutor, Map<String, Object> options) {
        Object elementOption = options.get("element");
        if (elementOption == null)
            return;

        // Case 1: User passed a WebElement directly — options.put("element",
        // webElement)
        if (elementOption instanceof WebElement) {
            Map<String, String> resolved = resolveToSelectorMap(jsExecutor, (WebElement) elementOption);
            if (resolved != null) {
                options.put("element", resolved);
            } else {
                log.warning("Failed to resolve WebElement for element screenshot");
            }
            return;
        }

    }

    /**
     * Resolves a single WebElement to a Map with either {id: "value"} or
     * {cssSelector: "value"}.
     */
    private static Map<String, String> resolveToSelectorMap(JavascriptExecutor jsExecutor, WebElement element) {
        try {
            String id = (String) jsExecutor.executeScript(ID_SCRIPT, element);
            if (id != null && !id.isEmpty()) {
                Map<String, String> result = new HashMap<>();
                result.put("id", id);
                return result;
            }

            String cssSelector = (String) jsExecutor.executeScript(CSS_SELECTOR_SCRIPT, element);
            if (cssSelector != null && !cssSelector.isEmpty()) {
                Map<String, String> result = new HashMap<>();
                result.put("cssSelector", cssSelector);
                return result;
            }
        } catch (StaleElementReferenceException e) {
            log.warning("Skipping stale WebElement for element screenshot: element is no longer attached to the DOM");
        } catch (Exception e) {
            log.warning("Failed to resolve WebElement for element screenshot: " + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void resolveDOMOption(JavascriptExecutor jsExecutor, Map<String, Object> options, String key) {
        Object domOption = options.get(key);
        if (domOption == null || !(domOption instanceof Map))
            return;

        Map<String, Object> domMap = (Map<String, Object>) domOption;

        // Handle "elements" key — a dedicated list of WebElement objects
        Object elementsObj = domMap.remove("elements");
        if (elementsObj instanceof List) {
            List<?> elements = (List<?>) elementsObj;
            for (Object item : elements) {
                if (item instanceof WebElement) {
                    resolveAndAdd(jsExecutor, (WebElement) item, domMap);
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
                    resolveAndAdd(jsExecutor, (WebElement) item, domMap);
                }
            }
            // Replace the mixed list with only the string values
            domMap.put(selectorKey, strings);
        }

        // Clean up empty lists
        domMap.entrySet().removeIf(entry -> entry.getValue() instanceof List && ((List<?>) entry.getValue()).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static void resolveAndAdd(JavascriptExecutor jsExecutor, WebElement element, Map<String, Object> domMap) {
        try {
            // Try to get element ID first
            String id = (String) jsExecutor.executeScript(ID_SCRIPT, element);
            if (id != null && !id.isEmpty()) {
                List<String> idList = (List<String>) domMap.computeIfAbsent("id", k -> new ArrayList<String>());
                if (!idList.contains(id)) {
                    idList.add(id);
                }
                return;
            }

            // Fall back to generating a unique CSS selector
            String cssSelector = (String) jsExecutor.executeScript(CSS_SELECTOR_SCRIPT, element);
            if (cssSelector != null && !cssSelector.isEmpty()) {
                List<String> cssList = (List<String>) domMap.computeIfAbsent("cssSelector",
                        k -> new ArrayList<String>());
                if (!cssList.contains(cssSelector)) {
                    cssList.add(cssSelector);
                }
            }
        } catch (StaleElementReferenceException e) {
            log.warning("Skipping stale WebElement in " + domMap + ": element is no longer attached to the DOM");
        } catch (Exception e) {
            log.warning("Failed to resolve WebElement to selector: " + e.getMessage());
        }
    }
}
