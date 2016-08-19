package org.samcrow.simplechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.Iterator;
import java.util.List;

/**
 * A scatter chart with lines connecting the points
 */
public class Chart<T> extends View {

    /**
     * The data model
     */
    private final DataModel<T> mData = new DataModel<>();

    /**
     * Paint used to draw the axes
     */
    private Paint mAxisPaint;

    /**
     * Paint used to draw the lines between points
     */
    private Paint mLinePaint;

    /**
     * Paint used to draw data points
     */
    private Paint mPointPaint;

    /**
     * Margin from the view edge to the chart
     */
    private final float mAxisMargin = dpToPixels(20);

    public Chart(Context context) {
        super(context);
        init(context);
    }

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Chart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mAxisPaint = new Paint();
        mAxisPaint.setColor(context.getResources().getColor(R.color.color_default_axis));
        mAxisPaint.setStrokeWidth(dpToPixels(2));
        mAxisPaint.setStyle(Style.STROKE);

        mLinePaint = new Paint();
        mLinePaint.setColor(context.getResources().getColor(R.color.color_default_data));
        mLinePaint.setStrokeWidth(dpToPixels(2));
        mLinePaint.setStyle(Style.STROKE);

        mPointPaint = new Paint();
        mPointPaint.setColor(context.getResources().getColor(R.color.color_default_data));
        mPointPaint.setStrokeWidth(dpToPixels(2));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // Draw chart boundary
        final RectF chartArea = new RectF(mAxisMargin, getHeight() - mAxisMargin, getWidth() - mAxisMargin, mAxisMargin);
        canvas.drawRect(chartArea, mAxisPaint);

        // Create a matrix mapping from chart coordinates to screen coordinates
        final RectF dataArea = new RectF(0, 0, mData.getMaxX(), mData.getMaxY());
        final Matrix chartInvert = new Matrix();
        final boolean setupResult = chartInvert.setRectToRect(dataArea, chartArea, ScaleToFit.FILL);
        if (!setupResult) {
            throw new RuntimeException("Matrix setup failed");
        }
        for (Iterator<Entry<T>> it = mData.entries(); it.hasNext(); ) {
            final Entry<T> item = it.next();
            final float[] itemPosition = new float[] { item.getX(), item.getY() };
            chartInvert.mapPoints(itemPosition);
            canvas.drawCircle(itemPosition[0], itemPosition[1], 10, mPointPaint);
        }


        canvas.drawText("Test", 50, convertY(50), mAxisPaint);
    }

    private float convertY(float coordinate) {
        return getHeight() - coordinate;
    }

    private float dpToPixels(float dp) {
        final DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public void setData(List<Entry<T>> data) {
        mData.setEntries(data);
    }

    public void clear() {
        mData.setEntries(null);
        invalidate();
    }
}
