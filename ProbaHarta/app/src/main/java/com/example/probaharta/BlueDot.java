package com.example.probaharta;
import android.content.Context;

import android.graphics.Canvas;

import android.graphics.Paint;

import android.graphics.Path;

import android.graphics.PointF;

import android.util.AttributeSet;

import android.util.Log;

import com.example.probaharta.R;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;



public class BlueDot extends SubsamplingScaleImageView
{
    private float Radius1= 1.0f;
    private float Radius2=1.0f;
    private PointF dotCenter = null;
    private double heading = -1.0;
    private BlueDotMoveEstimate bdm=new BlueDotMoveEstimate();
    Paint paint = new Paint();

    public void setRadius1(float Radius1)
    {
        this.Radius1 = Radius1;
    }
    public void setRadius2(float Radius2)
    {
        this.Radius2= Radius2;
    }
    public void setDotCenter(PointF dotCenter)
    {
        this.dotCenter=dotCenter;
    }
    public void setHeading(double heading)
    {
        this.heading=heading;
    }
    public BlueDot(Context context , AttributeSet attr)
    {
        super(context,attr);
        initialize();
    }
    private void initialize()
    {
        Log.d("BlueDot","Initialize");
        setWillNotDraw(false);
        setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_CENTER);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.ia_blue));

    }
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if(!isReady())
        {
            return;
        }
        if(dotCenter!= null) {
            BlueDotMoveEstimate.update(
                    dotCenter.x, dotCenter.y,
                    (float) ((heading) / 180.0 * Math.PI), //transform in radians;
                    Radius1, System.currentTimeMillis());


            PointF vPoint = sourceToViewCoord(BlueDotMoveEstimate.getX(), BlueDotMoveEstimate.getY());

            //cercmare-radius1;
            float scaledRadius1 = getScale() * BlueDotMoveEstimate.getRadius();
            paint.setAlpha(30);
            canvas.drawCircle(vPoint.x, vPoint.y, scaledRadius1, paint);

            //cerc mijloc
            float scaledRadius2 = getScale() * Radius2;
            paint.setAlpha(90);
            canvas.drawCircle(vPoint.x, vPoint.y, scaledRadius2, paint);
            //Paint triunghi directie
            if (heading != -1.0)
            {
                paint.setAlpha(225);
                Path triangle = headingTriangle(vPoint.x,vPoint.y,BlueDotMoveEstimate.getHeading()-(float)Math.PI/2,scaledRadius2);
                canvas.drawPath(triangle,paint);
            }

        }
        postInvalidate();
    }
    private static Path headingTriangle(float x, float y, float a, float r){

        float x1 = (float)(x + 0.9*r*Math.cos(a));
        float y1 = (float)(y + 0.9*r*Math.sin(a));
        float x2 = (float)(x + 0.2*r*Math.cos(a + 0.5*Math.PI));
        float y2 = (float)(y + 0.2*r*Math.sin(a + 0.5*Math.PI));
        float x3 = (float)(x + 0.2*r*Math.cos(a - 0.5*Math.PI));
        float y3 = (float)(y + 0.2*r*Math.sin(a - 0.5*Math.PI));

        Path triangle = new Path();
        triangle.moveTo(x1, y1);
        triangle.lineTo(x2, y2);
        triangle.lineTo(x3, y3);
        triangle.lineTo(x1, y1);

        return triangle;
    }
}
