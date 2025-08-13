package io.github.lambdatest.utils;

import java.util.Objects;

public class ElementBoundingBox {
    private String selectorKey; // Generic selector key (e.g., "xpath:value", "class:value")
    private String selectorType; // Type of selector (xpath, class, accessibilityid, name, id, css)
    private String selectorValue; // The actual selector value
    private String purpose; // "ignore" or "select"
    private int x;
    private int y;
    private int width;
    private int height;
    private int chunkIndex;
    private String platform;

    public ElementBoundingBox(String selectorKey, int x, int y, int width, int height, int chunkIndex, String platform, String purpose) {
        this.selectorKey = selectorKey;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.chunkIndex = chunkIndex;
        this.platform = platform;
        this.purpose = purpose;

        parseSelectorKey(selectorKey);
    }

    /**
     * Parse selectorKey to extract selectorType and selectorValue
     * Format: "type:value" (e.g., "xpath://button", "class:header")
     */
    private void parseSelectorKey(String selectorKey) {
        if (selectorKey != null && selectorKey.contains(":")) {
            String[] parts = selectorKey.split(":", 2);
            if (parts.length == 2) {
                this.selectorType = parts[0];
                this.selectorValue = parts[1];
            }
        }
    }


    public String getSelectorKey() { return selectorKey; }
    public String getPurpose() { return purpose; }

    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getChunkIndex() { return chunkIndex; }
    public String getPlatform() { return platform; }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ElementBoundingBox that = (ElementBoundingBox) obj;
        return Objects.equals(selectorKey, that.selectorKey) &&
               Math.abs(x - that.x) < 10 &&
               Math.abs(y - that.y) < 10;
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectorKey, x / 10, y / 10);
    }

    @Override
    public String toString() {
        return String.format("ElementBoundingBox{selector='%s', type='%s', value='%s', purpose='%s', position=(%d,%d), size=(%d,%d), chunk=%d, platform='%s'}", 
                           selectorKey, selectorType, selectorValue, purpose, x, y, width, height, chunkIndex, platform);
    }
} 