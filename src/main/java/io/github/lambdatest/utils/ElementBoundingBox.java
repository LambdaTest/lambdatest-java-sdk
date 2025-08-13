package io.github.lambdatest.utils;

import java.util.Objects;

/**
 * Immutable representation of an element's bounding box with selector information.
 * This class stores both raw selector key and parsed components for efficiency.
 */
public final class ElementBoundingBox {
    private static final String SELECTOR_DELIMITER = ":";
    private static final int COORDINATE_TOLERANCE = 10;
    private static final int MIN_DIMENSION = 0;

    private final String selectorKey;
    private final String selectorType;
    private final String selectorValue;
    private final String purpose;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int chunkIndex;
    private final String platform;
    private final long timestamp;

    public ElementBoundingBox(String selectorKey, int x, int y, int width, int height,
                              int chunkIndex, String platform, String purpose) {
        this.selectorKey = validateSelectorKey(selectorKey);
        this.x = x;
        this.y = y;
        this.width = validateDimension(width, "width");
        this.height = validateDimension(height, "height");
        this.chunkIndex = validateChunkIndex(chunkIndex);
        this.platform = validateNotNull(platform, "platform");
        this.purpose = validateNotNull(purpose, "purpose");
        this.timestamp = System.currentTimeMillis();

        SelectorComponents components = parseSelectorKey(selectorKey);
        this.selectorType = components.type;
        this.selectorValue = components.value;
    }



    private SelectorComponents parseSelectorKey(String selectorKey) {
        if (selectorKey == null || !selectorKey.contains(SELECTOR_DELIMITER)) {
            throw new IllegalArgumentException("Invalid selector key format. Expected 'type:value', got: " + selectorKey);
        }

        int delimiterIndex = selectorKey.indexOf(SELECTOR_DELIMITER);
        String type = selectorKey.substring(0, delimiterIndex).trim();
        String value = selectorKey.substring(delimiterIndex + 1).trim();

        if (type.isEmpty() || value.isEmpty()) {
            throw new IllegalArgumentException("Selector type and value cannot be empty. Got: " + selectorKey);
        }

        return new SelectorComponents(type, value);
    }

    // Validation methods
    private String validateSelectorKey(String selectorKey) {
        if (selectorKey == null || selectorKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Selector key cannot be null or empty");
        }
        return selectorKey.trim();
    }

    private String validateNotNull(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        return value;
    }

    private int validateDimension(int dimension, String dimensionName) {
        if (dimension < MIN_DIMENSION) {
            throw new IllegalArgumentException(dimensionName + " cannot be negative: " + dimension);
        }
        return dimension;
    }

    private int validateChunkIndex(int chunkIndex) {
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("Chunk index cannot be negative: " + chunkIndex);
        }
        return chunkIndex;
    }

    // Getters
    public String getSelectorKey() { return selectorKey; }
    public String getSelectorType() { return selectorType; }
    public String getSelectorValue() { return selectorValue; }
    public String getPurpose() { return purpose; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getChunkIndex() { return chunkIndex; }
    public String getPlatform() { return platform; }
    public long getTimestamp() { return timestamp; }

    // Utility methods
    public boolean isIgnoreElement() {
        return "ignore".equalsIgnoreCase(purpose);
    }

    public boolean isSelectElement() {
        return "select".equalsIgnoreCase(purpose);
    }

    public int getArea() {
        return width * height;
    }

    public int getCenterX() {
        return x + width / 2;
    }

    public int getCenterY() {
        return y + height / 2;
    }

    public boolean contains(int pointX, int pointY) {
        return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
    }

    public boolean overlaps(ElementBoundingBox other) {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y;
    }

    public ElementBoundingBox withOffset(int offsetX, int offsetY) {
        return new ElementBoundingBox(selectorKey, x + offsetX, y + offsetY, width, height, chunkIndex, platform, purpose);
    }

    public ElementBoundingBox withPurpose(String newPurpose) {
        return new ElementBoundingBox(selectorKey, x, y, width, height, chunkIndex, platform, newPurpose);
    }

    // Legacy method for backward compatibility - marked as deprecated
    @Deprecated
    public String getXpath() {
        return "xpath".equalsIgnoreCase(selectorType) ? selectorValue : "";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ElementBoundingBox that = (ElementBoundingBox) obj;
        return Objects.equals(selectorKey, that.selectorKey) &&
                isWithinTolerance(x, that.x) &&
                isWithinTolerance(y, that.y) &&
                width == that.width &&
                height == that.height &&
                chunkIndex == that.chunkIndex &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(purpose, that.purpose);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                selectorKey,
                normalizeCoordinate(x),
                normalizeCoordinate(y),
                width,
                height,
                chunkIndex,
                platform,
                purpose
        );
    }

    @Override
    public String toString() {
        return String.format(
                "ElementBoundingBox{selector='%s', type='%s', value='%s', purpose='%s', " +
                        "position=(%d,%d), size=(%dx%d), chunk=%d, platform='%s', timestamp=%d}",
                selectorKey, selectorType, selectorValue, purpose,
                x, y, width, height, chunkIndex, platform, timestamp
        );
    }

    private boolean isWithinTolerance(int value1, int value2) {
        return Math.abs(value1 - value2) <= COORDINATE_TOLERANCE;
    }

    private int normalizeCoordinate(int coordinate) {
        return coordinate / (COORDINATE_TOLERANCE + 1);
    }

    // Inner classes
    private static class SelectorComponents {
        final String type;
        final String value;

        SelectorComponents(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }
}