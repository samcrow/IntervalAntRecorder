package org.samcrow.simplechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * A scatter chart with lines connecting the points
 */
public class Chart extends View {

    /**
     * The data to display in the chart
     */
    private List<Entry<?>> mData;

    public Chart(Context context) {
        super(context);
    }

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Chart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final Paint paint = new Paint();
        canvas.drawText("Test", 50, 50, paint);
    }

    public void setData(List<Entry<?>> data) {
        mData = data;
    }
}
