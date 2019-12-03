package xyz.herelookingatyoukid.svgpainting.core.componet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import xyz.herelookingatyoukid.svgpainting.SVGPaintingViewListener;
import xyz.herelookingatyoukid.svgpainting.core.data.PathBean;
import xyz.herelookingatyoukid.svgpainting.core.data.PathHistory;
import xyz.herelookingatyoukid.svgpainting.core.helper.CommonMathHelper;
import xyz.herelookingatyoukid.svgpainting.core.helper.map.SVGBuilder;
import xyz.herelookingatyoukid.svgpainting.overlay.SVGPaintingBaseOverlay;


public class MapMainView extends SurfaceView implements Callback {

    private static final String TAG = "MapMainView";
    private static String STUDIO = "studio";
    private static String PICTURES = "pictures";
    // 用户画作（ArrayList<PathBean>）保存到本地的路径
    private static File studioPath;
    // 用户画作（以bitmap的形式）保存到本地的路径
    private static File picturesPath;

    private SVGPaintingViewListener mapViewListener = null;
    private SurfaceHolder surfaceHolder;
    private List<SVGPaintingBaseOverlay> layers;
    private MapOverlay mapOverlay;
    private SparkOverlay sparkOverlay;
    private ArrayList<PathBean> pathBeans = new ArrayList<>();

    private boolean isRotationGestureEnabled = false;
    private boolean isZoomGestureEnabled = true;
    private boolean isScrollGestureEnabled = true;
    private boolean isRotateWithTouchEventCenter = false;
    private boolean isZoomWithTouchEventCenter = false;
    private boolean isMapLoadFinsh = false;


    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLLING = 1;
    private static final int TOUCH_STATE_SCALING = 2;
    private static final int TOUCH_STATE_ROTATE = 3;
    private static final int TOUCH_STATE_POINTED = 4;
    private int mTouchState = MapMainView.TOUCH_STATE_REST;

    // 判断旋转和缩放的手势专用
    private float disX;
    // 判断的方式是夹角 钝角啊blablabla
    private float disY;
    // 三角形的三条边 用的是余弦定理
    private float disZ;
    private float lastX;
    private float lastY;

    // 默认的最小缩放
    private float minZoomValue = 0.8f;
    private float maxZoomValue = 5.0f;
    private boolean isFirstPointedMove = true;

    // 当前地图应用的矩阵变化
    private Matrix matrix = new Matrix();
    // 保存手势Down下时的矩阵
    private Matrix savedMatrix = new Matrix();

    // 手势触摸的起始点
    private PointF start = new PointF();
    // 双指手势的中心点
    private PointF mid = new PointF();

    private float firstDegrees;
    // 判断旋转和缩放的手势专用
    private float firstDistance;

    ExecutorService executor;
    Future future;


    private float rotateDegrees = 0f;
    private float currentRotateDegrees = 0f;
    private float zoom = 1f;
    private float currentZoom = 1f;

    private String currentPaintColor = "#666666";

    private Rect dirty = null;

    private SVGBuilder svgBuilder;
    // 后退一步
    private LinkedList<PathHistory> prePaths = new LinkedList<>();
    // 前进一步
    private LinkedList<PathHistory> postPaths = new LinkedList<>();
    private static final int MAX_HISTORY = 10;
    private int currentStep = 0;


    public MapMainView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MapMainView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initMapView();
    }

    private void initMapView() {
        checkAndInitDataPath();
        layers = new ArrayList<SVGPaintingBaseOverlay>() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean add(SVGPaintingBaseOverlay overlay) {
                synchronized (this) {
                    if (this.size() != 0) {
                        if (overlay.showLevel >= this.get(this.size() - 1).showLevel) {
                            super.add(overlay);
                        } else {
                            for (int i = 0; i < this.size(); i++) {
                                if (overlay.showLevel <= this.get(i).showLevel) {
                                    super.add(i, overlay);
                                    break;
                                }
                            }
                        }
                    } else {
                        super.add(overlay);
                    }

                }
                return true;
            }

            @Override
            public void clear() {
                super.clear();
                MapMainView.this.mapOverlay = null;
            }
        };
        getHolder().addCallback(this);
    }

    private void checkAndInitDataPath() {
        studioPath = new File(getContext().getFilesDir(), STUDIO);
        if (!studioPath.exists()) {
            studioPath.mkdirs();
        }
        picturesPath = new File(getContext().getFilesDir(), PICTURES);
        if (!picturesPath.exists()) {
            picturesPath.mkdirs();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.surfaceHolder = holder;
        if (dirty == null || dirty.bottom == 0 || dirty.right == 0) {
            dirty = new Rect(0, 0, this.getWidth(), this.getHeight());
        }
        if (surfaceHolder != null) {
            this.refresh();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.surfaceHolder = holder;
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(-1);
        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void refresh() {
        try {
            if (surfaceHolder != null) {
                synchronized (this.surfaceHolder) {
                    Canvas canvas = surfaceHolder.lockCanvas(dirty);
                    if (canvas != null) {
                        canvas.drawColor(-1);
                        for (int i = 0; i < layers.size(); i++) {
                            if (layers.get(i).isVisible) {
                                layers.get(i).draw(canvas, matrix, currentZoom, currentRotateDegrees);
                            }
                        }
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isMapLoadFinsh || mapOverlay == null) {
            return false;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                this.mTouchState = TOUCH_STATE_SCROLLING;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    this.mTouchState = TOUCH_STATE_POINTED;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                isFirstPointedMove = true;
                this.refresh();
                if (this.mTouchState == TOUCH_STATE_SCALING) {
                    this.zoom = this.currentZoom;
                } else if (this.mTouchState == TOUCH_STATE_ROTATE) {
                    this.rotateDegrees = this.currentRotateDegrees;
                } else if (withFloorPlan(event.getX(), event.getY()) && event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        for (int i = 0; i < layers.size(); i++) {
                            layers.get(i).onTap(event);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

//                if (!isRotationGestureEnabled) {
//                    // 調整地圖的位置居中顯示
//                    mapCenter(true, true);
//                }
                responseChangeColor(event);
                this.mTouchState = TOUCH_STATE_REST;
                break;

            case MotionEvent.ACTION_MOVE:
                if (this.mTouchState == TOUCH_STATE_POINTED) {
                    if (isFirstPointedMove) {
                        midPoint(mid, event);
                        lastX = event.getX(0);
                        lastY = event.getY(0);
                        disX = CommonMathHelper.getDistanceBetweenTwoPoints(event.getX(0), event.getY(0), mid.x, mid.y);
                        isFirstPointedMove = false;
                    } else {
                        savedMatrix.set(matrix);
                        disY = CommonMathHelper.getDistanceBetweenTwoPoints(lastX, lastY, event.getX(0), event.getY(0));
                        disZ = CommonMathHelper.getDistanceBetweenTwoPoints(mid.x, mid.y, event.getX(0), event.getY(0));
                        if (justRotateGesture()) {
                            firstDegrees = rotation(event);
                            this.mTouchState = TOUCH_STATE_ROTATE;
                        } else {
                            firstDistance = spaceBetweenTwoEvents(event);
                            this.mTouchState = TOUCH_STATE_SCALING;
                        }
                    }
                } else if (this.mTouchState == TOUCH_STATE_SCALING) {
                    if (this.isZoomGestureEnabled) {
                        matrix.set(savedMatrix);
                        if (isZoomWithTouchEventCenter) {
                            midPoint(mid, event);
                        } else {
                            mid.x = this.getWidth() / 2;
                            mid.y = this.getHeight() / 2;
                        }
                        float sencondDistance = spaceBetweenTwoEvents(event);
                        float scale = sencondDistance / firstDistance;
                        float ratio = this.zoom * scale;
                        if (ratio < minZoomValue) {
                            ratio = minZoomValue;
                            scale = ratio / this.zoom;
                        } else if (ratio > maxZoomValue) {
                            ratio = maxZoomValue;
                            scale = ratio / this.zoom;
                        }
                        this.currentZoom = ratio;
                        this.matrix.postScale(scale, scale, mid.x, mid.y);
                        this.refresh();
                    }
                } else if (this.mTouchState == TOUCH_STATE_ROTATE) {
                    if (this.isRotationGestureEnabled) {
                        matrix.set(savedMatrix);
                        if (isRotateWithTouchEventCenter) {
                            midPoint(mid, event);
                        } else {
                            mid.x = this.getWidth() / 2;
                            mid.y = this.getHeight() / 2;
                        }
                        float deltaDegrees = rotation(event) - firstDegrees;
                        float tempD = (this.rotateDegrees + deltaDegrees) % 360;
                        this.currentRotateDegrees = tempD > 0 ? tempD : 360 + tempD;
                        this.matrix.postRotate(deltaDegrees, mid.x, mid.y);
                        this.refresh();
                    }
                } else if (this.mTouchState == TOUCH_STATE_SCROLLING) {
                    if (this.isScrollGestureEnabled) {
                        matrix.set(savedMatrix);
                        float currentOffsetX = event.getX() - start.x;
                        float currentOffsetY = event.getY() - start.y;
                        matrix.postTranslate(currentOffsetX, currentOffsetY);
                        fixTranslate();
                        refresh();
                    }
                }
                break;
            default:
        }
        return true;
    }

    /**
     * 修正translate的最大值 防止视图滑出屏幕
     */
    private void fixTranslate() {
        float[] v = new float[9];
        matrix.getValues(v);
        float afx = v[Matrix.MTRANS_X];
        float afy = v[Matrix.MTRANS_Y];
        float fixX = 0;
        float fixY = 0;
        int maxTransX = getMeasuredWidth() / 2;
        int maxTransY = getMeasuredHeight() - mapOverlay.getFloorMap().getHeight();
        Log.d("fixTranslate", "afy = " + afy + " maxTransY = " + maxTransY);
        if (afx > maxTransX) {
            fixX = maxTransX - afx;
        } else if (afx < -maxTransX) {
            fixX = -afx - maxTransX;
        }
        if (afy > maxTransY) {
            fixY = maxTransY - afy;
        } else if (afy <= 0) {
            fixY = -afy;
        }
        matrix.postTranslate(fixX, fixY);
    }

    public void responseChangeColor(MotionEvent motionEvent) {
        float[] src = {motionEvent.getX(), motionEvent.getY()};
        float[] dst = new float[2];
        Matrix matrix2 = new Matrix();
        // 当前矩阵的逆矩阵
        this.matrix.invert(matrix2);
        // 获取按照矩阵变换后的坐标点
        matrix2.mapPoints(dst, src);

        if (responseColor(dst)) {
            reDraw();
            return;
        }
        if (responseColor(new float[]{dst[0] - 5.0f, dst[1]})) {
            reDraw();
            return;
        }
        if (responseColor(new float[]{dst[0], dst[1] - 5.0f})) {
            reDraw();
            return;
        }
        if (responseColor(new float[]{dst[0] + 5.0f, dst[1]})) {
            reDraw();
            return;
        }
        if (responseColor(new float[]{dst[0], dst[1] + 5.0f})) {
            reDraw();
        }
    }

    public boolean responseColor(float[] dstPosition) {
        PathBean pathItem;
        boolean flag;
        boolean z2 = false;
        ArrayList<PathBean> arrayList = pathBeans;
        int size = arrayList.size() - 1;
        while (true) {
            if (size < 0) {
                flag = false;
                break;
            }
            pathItem = arrayList.get(size);
            Path path = pathItem.getPath();
            if (path != null) {
                int i = (int) dstPosition[0];
                int i2 = (int) dstPosition[1];
                int i3 = i + 1;
                int i4 = i2 + 1;
                if (isTouch(i, i2, path) || isTouch(i3, i2, path) || isTouch(i, i4, path) || isTouch(i3, i4, path)) {
                    if (!pathItem.isEnableFillColor()) {
                        if (!pathItem.isShade() && pathItem.isStroke()) {
                            pathItem = null;
                            flag = true;
                            z2 = true;
                            break;
                        }
                    } else {
                        if (!pathItem.getGroupName().toLowerCase().contains("bg")) {
                            int color = getPaintColor();
                            int oldColor = pathItem.getFillColor();
                            if (color != pathItem.getFillColor()) {
                                pathItem.setFillColor(color);
                            } else {
                                pathItem.setFillColor(-1);
                            }
                            updatePreSteps(size, pathItem, oldColor);
                            pathItem = null;
                        }
                        flag = true;
                    }
                }
            }
            size--;
        }

        if (!flag) {
            int size3 = arrayList.size() - 1;
            while (true) {
                if (size3 < 0) {
                    break;
                }
                PathBean pathItem3 = arrayList.get(size3);
                RectF rect = pathItem3.getRect();
                if (rect != null) {
                    if (isTouch((int) dstPosition[0], (int) dstPosition[1], rect)) {
                        if (pathItem3.isEnableFillColor()) {
                            int color3 = getPaintColor();
                            int oldColor = pathItem3.getFillColor();
                            if (color3 != pathItem3.getFillColor()) {
                                pathItem3.setFillColor(color3);
                            } else {
                                pathItem3.setFillColor(-1);
                            }
                            updatePreSteps(size, pathItem3, oldColor);
                        } else if (pathItem3.isShade()) {
                            continue;
                        }
                    }
                    if (pathItem3.isStroke()) {
                        z2 = true;
                        break;
                    }
                }
                size3--;
            }
        }
        return !z2;

    }

    private void updatePreSteps(int position, PathBean path, int oldColor) {
        checkHistoryState();
        if (prePaths.size() == MAX_HISTORY) {
            prePaths.removeLast();
        }
        PathBean oldColorPath = new PathBean();
        oldColorPath.setFillColor(oldColor);
        oldColorPath.setEnableFillColor(path.isEnableFillColor());
        oldColorPath.setGroupName(path.getGroupName());
        oldColorPath.setLocalName(path.getLocalName());
        oldColorPath.setPath(path.getPath());
        oldColorPath.setRect(path.getRect());
        oldColorPath.setShade(path.isShade());
        oldColorPath.setStroke(path.isStroke());
        prePaths.addFirst(new PathHistory(position, oldColorPath));
    }

    private void checkHistoryState() {
        if (currentStep != 0) {
            while (currentStep > 0) {
                prePaths.removeFirst();
                currentStep--;
            }
            postPaths.clear();
            currentStep = 0;
        }
    }

    public void preStep() {
        if (currentStep == MAX_HISTORY - 1 || currentStep >= prePaths.size()) {
            Log.d(TAG, "current step " + currentStep);
            return;
        }
        // 移除上一次状态
        int index = prePaths.get(currentStep).getIndex();
        PathBean old = pathBeans.remove(index);
        // 添加上一次状态（未改变之前的颜色）
        pathBeans.add(index, prePaths.get(currentStep).getPath());
        currentStep++;
        updatePostSteps(currentStep, old, index);
        reDraw();
        Log.d(TAG, "current step " + currentStep);
    }

    private void updatePostSteps(int position, PathBean path, int pathIndex) {
        while (postPaths.size() >= position) {
            postPaths.removeLast();
        }
        postPaths.addLast(new PathHistory(pathIndex, path));
    }

    public void postStep() {
        if (currentStep == 0) {
            Log.d(TAG, "current step 0");
            return;
        }
        currentStep--;
        int index = prePaths.get(currentStep).getIndex();
        pathBeans.remove(index);
        pathBeans.add(index, postPaths.get(currentStep).getPath());
        postPaths.remove(currentStep);
        reDraw();
        Log.d(TAG, "current step " + currentStep);
    }

    private int getPaintColor() {
        return Color.parseColor(currentPaintColor);
    }

    public void setPaintColor(String color) {
        currentPaintColor = color;
    }

    private boolean isTouch(int i, int i2, Path path) {
        Region region = new Region();
        RectF rectF = new RectF();
        path.computeBounds(rectF, true);
        region.setPath(path, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
        return region.contains(i, i2);
    }

    public static boolean isTouch(int i, int i2, RectF rectF) {
        return rectF.contains((float) i, (float) i2);
    }

    public void onDestroy() {
        try {
            for (int i = 0; i < layers.size(); i++) {
                layers.get(i).onDestroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }
    }

    public void onPause() {
        try {
            for (int i = 0; i < layers.size(); i++) {
                layers.get(i).onPause();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onResume() {
        try {
            for (int i = 0; i < layers.size(); i++) {
                layers.get(i).onResume();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public float getCurrentRotateDegrees() {
        return this.currentRotateDegrees;
    }

    public void setCurrentRotationDegrees(float degrees, float pivotX, float pivotY) {
        if (isRotationGestureEnabled) {
            this.matrix.postRotate(-currentRotateDegrees + degrees, pivotX, pivotY);
            this.rotateDegrees = this.currentRotateDegrees = degrees;
            setCurrentRotateDegreesWithRule();
            refresh();
            mapCenter(true, true);
        }
    }

    public float getCurrentZoomValue() {
        return this.currentZoom;
    }

    public void setCurrentZoomValue(float zoom, float pivotX, float pivotY) {
        this.matrix.postScale(zoom / currentZoom, zoom / currentZoom, pivotX, pivotY);
        this.zoom = this.currentZoom = zoom;
        this.refresh();
    }

    public float getMaxZoomValue() {
        return maxZoomValue;
    }

    public void setMaxZoomValue(float maxZoomValue) {
        this.maxZoomValue = maxZoomValue;
    }

    public List<SVGPaintingBaseOverlay> getOverLays() {
        return this.layers;
    }

    public void translateBy(float x, float y) {
        this.matrix.postTranslate(x, y);
    }

    public float getMinZoomValue() {
        return minZoomValue;
    }

    public void setMinZoomValue(float minZoomValue) {
        this.minZoomValue = minZoomValue;
    }


    public float[] getMapCoordinateWithScreenCoordinate(float x, float y) {
        Matrix invertMatrix = new Matrix();
        float returnValue[] = {x, y};
        this.matrix.invert(invertMatrix);
        invertMatrix.mapPoints(returnValue);
        return returnValue;
    }

    public void registerMapViewListener(SVGPaintingViewListener mapViewListener) {
        this.mapViewListener = mapViewListener;
    }

    public void reDraw() {
        Picture newPicture = this.svgBuilder.svgHandler.reDraw();
        if (newPicture != null) {
            this.mapOverlay.setData(newPicture);
        }
    }

    public void loadMap(final String svgName, final String svgString) {
        isMapLoadFinsh = false;
        svgBuilder = new SVGBuilder();
        executor = Executors.newFixedThreadPool(3);
        future = executor.submit(new Runnable() {
            @Override
            public void run() {
                Picture picture = svgBuilder.readFromString(svgString).build().getPicture();
                if (picture != null) {
                    if (MapMainView.this.mapOverlay == null) {
                        MapMainView.this.mapOverlay = new MapOverlay(MapMainView.this);
                        MapMainView.this.getOverLays().add(mapOverlay);
                    }
                    pathBeans = svgBuilder.svgHandler.pathBeans;
                    if (checkLocalDoesHaveOldData(svgName)) {
                        readFromLocal(svgName);
                    }
                    MapMainView.this.mapOverlay.setData(picture);
                    Log.i(TAG, "mapLoadFinished");
                    if (mapViewListener != null) {
                        mapViewListener.onMapLoadComplete();
                    }
                    isMapLoadFinsh = true;
                } else {
                    if (mapViewListener != null) {
                        mapViewListener.onMapLoadError();
                    }
                }
            }

            private boolean checkLocalDoesHaveOldData(String svgName) {
                File file = new File(studioPath, svgName);
                return file.exists();
            }
        });
    }

    public void setRotationGestureEnabled(boolean enabled) {
        this.isRotationGestureEnabled = enabled;
    }

    public void setZoomGestureEnabled(boolean enabled) {
        this.isZoomGestureEnabled = enabled;
    }

    public void setScrollGestureEnabled(boolean enabled) {
        this.isScrollGestureEnabled = enabled;
    }

    public void setRotateWithTouchEventCenter(boolean isRotateWithTouchEventCenter) {
        this.isRotateWithTouchEventCenter = isRotateWithTouchEventCenter;
    }

    public void setZoomWithTouchEventCenter(boolean isZoomWithTouchEventCenter) {
        this.isZoomWithTouchEventCenter = isZoomWithTouchEventCenter;
    }

    public boolean isMapLoadFinish() {
        return this.isMapLoadFinsh;
    }


    public void sparkAtPoint(PointF point, float radius, int color, int repeatTimes) {
        sparkOverlay = new SparkOverlay(this, radius, point, color, repeatTimes);
        this.layers.add(sparkOverlay);
    }

    public void getCurrentMap() {
        try {
            Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas bitCanvas = new Canvas(bitmap);
            for (SVGPaintingBaseOverlay layer : layers) {
                layer.draw(bitCanvas, matrix, currentZoom, currentRotateDegrees);
            }
            if (mapViewListener != null) {
                mapViewListener.onGetCurrentMap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * **********************************************************************
     */

    // 地图居中显示
    private void mapCenter(boolean horizontal, boolean vertical) {
        Matrix m = new Matrix();
        m.set(matrix);
        RectF mapRect = new RectF(0, 0, this.mapOverlay.getFloorMap().getWidth(), this.mapOverlay.getFloorMap().getHeight());
        m.mapRect(mapRect);
        float width = mapRect.width();
        float height = mapRect.height();
        float deltaX = 0;
        float deltaY = 0;
        if (vertical) {
            if (height < this.getHeight()) {
                deltaY = (getHeight() - height) / 2 - mapRect.top;
            } else if (mapRect.top > 0) {
                deltaY = -mapRect.top;
            } else if (mapRect.bottom < getHeight()) {
                deltaY = getHeight() - mapRect.bottom;
            }
        }

        if (horizontal) {
            if (width < getWidth()) {
                deltaX = (getWidth() - width) / 2 - mapRect.left;

            } else if (mapRect.left > 0) {
                deltaX = -mapRect.left;
            } else if (mapRect.right < getWidth()) {
                deltaX = getWidth() - mapRect.right;
            }
            matrix.postTranslate(deltaX, deltaY);
        }
        refresh();
    }

    private float rotation(MotionEvent event) {
        float delta_x = (event.getX(0) - event.getX(1));
        float delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private float spaceBetweenTwoEvents(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private boolean justRotateGesture() {
        if (!this.isRotationGestureEnabled) {
            return false;
        }
        float cos = (disX * disX + disY * disY - disZ * disZ) / (2 * disX * disY);
        if (Float.isNaN(cos)) {
            return false;
        }
        if (Math.acos(cos) * (180 / Math.PI) < 120 && Math.acos(cos) * (180 / Math.PI) > 45)
            if (Math.acos(cos) * (180 / Math.PI) < 120 && Math.acos(cos) * (180 / Math.PI) > 45) {
                return true;
            }
        return false;
    }

    private float[] getHorizontalDistanceWithRotateDegree(float degrees, float x, float y) {
        float[] goal = new float[2];
        double f = Math.PI * (degrees / 180.0F);
        goal[0] = (float) (x * Math.cos(f) - y * Math.sin(f));
        goal[1] = (float) (x * Math.sin(f) + y * Math.cos(f));
        return goal;
    }


    private void setCurrentRotateDegreesWithRule() {
        if (getCurrentRotateDegrees() > 360) {
            this.currentRotateDegrees = getCurrentRotateDegrees() % 360;
        } else if (getCurrentRotateDegrees() < 0) {
            this.currentRotateDegrees = 360 + (getCurrentRotateDegrees() % 360);
        }
    }

    /**
     * point is/not in floor plan
     *
     * @param x
     * @param y
     * @return
     */
    public boolean withFloorPlan(float x, float y) {
        float[] goal = getMapCoordinateWithScreenCoordinate(x, y);
        return goal[0] > 0 && goal[0] < mapOverlay.getFloorMap().getWidth() && goal[1] > 0
                && goal[1] < mapOverlay.getFloorMap().getHeight();
    }

    public Bitmap getBitmap() {
        if (mapOverlay == null) {
            return null;
        }
        return this.mapOverlay.pictureDrawable2Bitmap();
    }

    public void saveBitmapToLocal(final String fileName) {
        // 在主线程获取bitmap 为的是执行保存任务时（异步时） bitmap已经获取成功
        // 异步获取bitmap有可能当前activity已执行onDestory() bitmap对象获取为null
        final Bitmap bitmap = getBitmap();
        if (bitmap == null) {
            return;
        }
        executor.submit(new Runnable() {
            @Override
            public void run() {
                FileOutputStream fileOutputStream = null;
                try {
                    File file = new File(picturesPath, fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fileOutputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // 图像保存好后发送广播 通知刷新
//                    Intent saveCompleteIntent = new Intent(BuildConfig.IMAGE_SAVE_COMPLETE);
//                    getContext().sendBroadcast(saveCompleteIntent);
                    bitmap.recycle();
                }
            }
        });
    }

    public void saveToLocal(final String fileName) {
        if (executor == null) {
            return;
        }
        executor.submit(new Runnable() {
            @Override
            public void run() {
                FileOutputStream fileOutputStream = null;
                ObjectOutputStream objectOutputStream = null;
                ArrayList<PathBean> arrayList = pathBeans;
                ArrayList<PathBean> arrayList2 = new ArrayList<>();
                for (int i = 0; i < arrayList.size(); i++) {
                    PathBean pathItem = arrayList.get(i);
                    PathBean baseItem = new PathBean();
                    baseItem.setEnableFillColor(pathItem.isEnableFillColor());
                    baseItem.setFillColor(pathItem.getFillColor());
                    baseItem.setGroupName(pathItem.getGroupName());
                    baseItem.setLocalName(pathItem.getLocalName());
                    baseItem.setShade(pathItem.isShade());
                    baseItem.setStroke(pathItem.isStroke());
                    arrayList2.add(baseItem);
                }
                File file = new File(studioPath, fileName);
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                try {
                    fileOutputStream = new FileOutputStream(file.toString());
                    objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(arrayList2);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (objectOutputStream != null) {
                        try {
                            objectOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public void readFromLocal(String fileName) {
        FileInputStream fileInputStream = null;
        ObjectInputStream objectInputStream = null;
        ArrayList localData = null;
        ArrayList<PathBean> target = pathBeans;
        File file = new File(studioPath, fileName);
        if (file.exists()) {
            try {
                fileInputStream = new FileInputStream(file.toString());
                objectInputStream = new ObjectInputStream(fileInputStream);
                localData = (ArrayList) objectInputStream.readObject();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (localData == null || target.size() != localData.size()) {
                try {
                    if (objectInputStream != null) {
                        objectInputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                return;
            }
            for (int i = 0; i < target.size(); i++) {
                PathBean pathItem = target.get(i);
                PathBean localItem = (PathBean) localData.get(i);
                pathItem.setLocalName(localItem.getLocalName());
                pathItem.setShade(localItem.isShade());
                pathItem.setGroupName(localItem.getGroupName());
                pathItem.setStroke(localItem.isStroke());
                pathItem.setFillColor(localItem.getFillColor());
                pathItem.setEnableFillColor(localItem.isEnableFillColor());
            }
            try {
                objectInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileInputStream.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            reDraw();
        }
    }
}
