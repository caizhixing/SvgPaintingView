package xyz.herelookingatyoukid.svgpainting;


import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import java.util.List;

import xyz.herelookingatyoukid.svgpainting.core.componet.MapMainView;
import xyz.herelookingatyoukid.svgpainting.overlay.SVGPaintingBaseOverlay;


public class SVGPaintingView extends FrameLayout {
    private MapMainView mapMainView;

    private SVGPaintingController mapController;

    private ImageView brandImageView;

    public SVGPaintingView(Context context) {
        this(context, null);
    }

    public SVGPaintingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SVGPaintingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mapMainView = new MapMainView(context, attrs, defStyle);
        addView(mapMainView);
        brandImageView = new ImageView(context, attrs, defStyle);
        brandImageView.setScaleType(ScaleType.FIT_START);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, context.getResources().getDisplayMetrics()));
        params.gravity = Gravity.BOTTOM | Gravity.LEFT;
        params.leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
        params.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics());
        addView(brandImageView, params);
    }

    /**
     * @return the map controller.
     */
    public SVGPaintingController getController() {
        if (this.mapController == null) {
            this.mapController = new SVGPaintingController(this);
        }
        return this.mapController;
    }

    public void registerMapViewListener(SVGPaintingViewListener idrMapViewListener) {
        this.mapMainView.registerMapViewListener(idrMapViewListener);
    }

    public void loadMap(String svgName,String svgString) {
        this.mapMainView.loadMap(svgName,svgString);
    }

    public void setBrandBitmap(Bitmap bitmap) {
        this.brandImageView.setImageBitmap(bitmap);
    }

    public void refresh() {
        this.mapMainView.refresh();
    }

    /**
     * @return whether the map is already loaded.
     */
    public boolean isMapLoadFinsh() {
        return this.mapMainView.isMapLoadFinish();
    }

    /**
     * get the current map.
     * It will be callback in the map listener of 'onGetCurrentMap'
     */
    public void getCurrentMap() {
        this.mapMainView.getCurrentMap();
    }


    public float getCurrentRotateDegrees() {
        return this.mapMainView.getCurrentRotateDegrees();
    }


    public float getCurrentZoomValue() {
        return this.mapMainView.getCurrentZoomValue();
    }


    public float getMaxZoomValue() {
        return this.mapMainView.getMaxZoomValue();
    }


    public float getMinZoomValue() {
        return this.mapMainView.getMinZoomValue();
    }


    public float[] getMapCoordinateWithScreenCoordinate(float screenX, float screenY) {
        return this.mapMainView.getMapCoordinateWithScreenCoordinate(screenX, screenY);
    }

    public List<SVGPaintingBaseOverlay> getOverLays() {
        return this.mapMainView.getOverLays();
    }


    public void onDestroy() {
        this.mapMainView.onDestroy();
    }

    public void onPause() {
        this.mapMainView.onPause();
    }

    public void onResume() {
        this.mapMainView.onResume();
    }

    public void setPaintColor(String color){
        this.mapMainView.setPaintColor(color);
    }

    public Bitmap getBitmap(){
        return this.mapMainView.getBitmap();
    }

    public void saveBitmapToLocal(String fileName){
        this.mapMainView.saveBitmapToLocal(fileName);
    }

    public void saveToLocal(String fileName){
        this.mapMainView.saveToLocal(fileName);
    }

    public void preStep(){
        this.mapMainView.preStep();
    }

    public void postStep(){
        this.mapMainView.postStep();
    }
}
