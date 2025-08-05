package io.github.lambdatest.utils;

import java.util.Objects;

public class ElementBoundingBox {
    private String xpath;
    private int x;
    private int y;
    private int width;
    private int height;
    private int chunkIndex;
    private long timestamp;
    private String platform;

    public ElementBoundingBox(String xpath, int x, int y, int width, int height, int chunkIndex, String platform) {
        this.xpath = xpath;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.chunkIndex = chunkIndex;
        this.timestamp = System.currentTimeMillis();
        this.platform = platform;
    }

    // Getters
    public String getXpath() { return xpath; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getChunkIndex() { return chunkIndex; }
    public long getTimestamp() { return timestamp; }
    public String getPlatform() { return platform; }

    // Calculate center point for deduplication
    public int getCenterX() { return x + width / 2; }
    public int getCenterY() { return y + height / 2; }

    // Calculate distance to another element center
    public double distanceTo(ElementBoundingBox other) {
        int dx = this.getCenterX() - other.getCenterX();
        int dy = this.getCenterY() - other.getCenterY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ElementBoundingBox that = (ElementBoundingBox) obj;
        return Objects.equals(xpath, that.xpath) &&
               Math.abs(x - that.x) < 10 &&
               Math.abs(y - that.y) < 10;
    }

    @Override
    public int hashCode() {
        return Objects.hash(xpath, x / 10, y / 10);
    }

    @Override
    public String toString() {
        return String.format("ElementBoundingBox{xpath='%s', position=(%d,%d), size=(%d,%d), chunk=%d, platform='%s'}", 
                           xpath, x, y, width, height, chunkIndex, platform);
    }
} 