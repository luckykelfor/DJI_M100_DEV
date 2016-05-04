package com.whu.m100;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DrawView extends View {

    private int lux;
    private int luy;
    private int rux;
    private int ruy;
    private int ldx;
    private int ldy;
    private int rdx;
    private int rdy;
    private int id;

    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setXY(int _lux, int _luy, int _rux, int _ruy, int _ldx, int _ldy, int _rdx, int _rdy, int _id) {
        lux = _lux;
        luy = _luy;
        rux = _rux;
        ruy = _ruy;
        ldx = _ldx;
        ldy = _ldy;
        rdx = _rdx;
        rdy = _rdy;
        id = _id;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(3f);
        canvas.drawLine(lux, luy, rux, ruy, paint);
        canvas.drawLine(lux, luy, ldx, ldy, paint);
        canvas.drawLine(ldx, ldy, rdx, rdy, paint);
        canvas.drawLine(rux, ruy, rdx, rdy, paint);
        canvas.drawLine(lux, luy, rdx, rdy, paint);
        canvas.drawLine(rux, ruy, ldx, ldy, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(1f);
        paint.setTextSize(60);
        
        float cx = (lux + rux + ldx + rdx) / 4;
        float cy = (luy + ruy + ldy + rdy) / 4;
        canvas.drawText(String.valueOf(id), cx, cy, paint);
    }

}

