package xyz.herelookingatyoukid.svgpainting.core.data;

/**
 * author : caizhixing
 * date : 2019/11/25
 */
public class PathHistory {
    private int index;
    private PathBean path;

    public PathHistory(int index, PathBean path) {
        this.index = index;
        this.path = path;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public PathBean getPath() {
        return path;
    }

    public void setPath(PathBean path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "PathHistory{" +
                "index=" + index +
                ", path=" + path +
                '}';
    }
}
