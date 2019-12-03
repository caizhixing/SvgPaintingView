package xyz.herelookingatyoukid.svgpainting.core.componet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.RectF;
import android.graphics.drawable.PictureDrawable;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;

import xyz.herelookingatyoukid.svgpainting.overlay.SVGPaintingBaseOverlay;


public class MapOverlay extends SVGPaintingBaseOverlay {
    private MapMainView mapMainView;
    private Picture floorMap;
    private Paint paint;
    private boolean hasMeasured;
    private boolean isFirstDraw = true;

    private static final String TAG = "MapLayer";

    public MapOverlay(MapMainView mapMainView) {
        this.mapMainView = mapMainView;
        this.paint = new Paint();
        this.showLevel = MAP_LEVEL;
    }

    public void setData(Picture floorMap) {
        this.floorMap = floorMap;
        if (this.mapMainView.getWidth() == 0) {
            ViewTreeObserver vto = this.mapMainView.getViewTreeObserver();
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (!hasMeasured) {
                        calcRatio();
                    }
                    return true;
                }
            });
        } else {
            calcRatio();
        }
    }

    public Bitmap pictureDrawable2Bitmap() {
        if (floorMap == null) {
            return null;
        }
        PictureDrawable pd = new PictureDrawable(floorMap);
        Bitmap bitmap = Bitmap.createBitmap(pd.getIntrinsicWidth(), pd.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawPicture(pd.getPicture());
        return bitmap;
    }

    public Picture getFloorMap() {
        return this.floorMap;
    }


    private void calcRatio() {
        this.mapMainView.refresh();
        hasMeasured = true;
    }

    private float getInitScale(float width, float height, float imageWidth, float imageHeight) {
        float widthRatio = width / imageWidth;
        float heightRatio = height / imageHeight;
        if (widthRatio * imageHeight <= height) {
            return widthRatio;
        } else if (heightRatio * imageWidth <= width) {
            return heightRatio;
        }
        return 0;
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onTap(MotionEvent event) {

    }

    @Override
    public void onDestroy() {
        this.floorMap = null;
    }

    @Override
    public void draw(Canvas canvas, Matrix matrix, float currentZoom, float currentRotateDegrees) {
        if (isFirstDraw) {
            isFirstDraw = false;
            mapCenter(matrix);
        }
        canvas.save();
        canvas.setMatrix(matrix);
        if (floorMap != null) {
            canvas.drawPicture(floorMap);
        }
        canvas.restore();
    }

    private void mapCenter(Matrix matrix) {
        Matrix m = new Matrix();
        m.set(matrix);
        RectF mapRect = new RectF(0, 0, getFloorMap().getWidth(), getFloorMap().getHeight());
        m.mapRect(mapRect);
        float width = mapRect.width();
        float height = mapRect.height();
        float deltaX = 0;
        float deltaY = 0;
        if (height < mapMainView.getHeight()) {
            deltaY = (mapMainView.getHeight() - height) / 2 - mapRect.top;
        } else if (mapRect.top > 0) {
            deltaY = -mapRect.top;
        } else if (mapRect.bottom < mapMainView.getHeight()) {
            deltaY = mapMainView.getHeight() - mapRect.bottom;
        }

        if (width < mapMainView.getWidth()) {
            deltaX = (mapMainView.getWidth() - width) / 2 - mapRect.left;

        } else if (mapRect.left > 0) {
            deltaX = -mapRect.left;
        } else if (mapRect.right < mapMainView.getWidth()) {
            deltaX = mapMainView.getWidth() - mapRect.right;
        }
        matrix.postTranslate(deltaX, deltaY);
    }
}
