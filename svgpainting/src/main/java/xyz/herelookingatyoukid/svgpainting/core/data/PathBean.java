package xyz.herelookingatyoukid.svgpainting.core.data;

import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.Serializable;

/**
 * author : caizhixing
 * date : 2019/11/14
 */
public class PathBean implements Serializable {

    private boolean enableFillColor;
    private int fillColor;
    private String groupName;
    private String localName;
    private boolean shade;
    private boolean stroke;
    private Path path;
    private RectF rect;

    public PathBean() {
    }

    public boolean isEnableFillColor() {
        return enableFillColor;
    }

    public void setEnableFillColor(boolean enableFillColor) {
        this.enableFillColor = enableFillColor;
    }

    public int getFillColor() {
        return fillColor;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public boolean isShade() {
        return shade;
    }

    public void setShade(boolean shade) {
        this.shade = shade;
    }

    public boolean isStroke() {
        return stroke;
    }

    public void setStroke(boolean stroke) {
        this.stroke = stroke;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public RectF getRect() {
        return rect;
    }

    public void setRect(RectF rect) {
        this.rect = rect;
    }

    @Override
    public String toString() {
        return "PathBean{" +
                "enableFillColor=" + enableFillColor +
                ", fillColor=" + fillColor +
                ", groupName='" + groupName + '\'' +
                ", localName='" + localName + '\'' +
                ", shade=" + shade +
                ", stroke=" + stroke +
                ", path=" + path +
                ", rect=" + rect +
                '}';
    }
}
