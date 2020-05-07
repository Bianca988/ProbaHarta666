package com.example.probaharta;

public  class BlueDotMoveEstimate {

    private static float x = 0f; // coordinate x

    private static float y = 0f; // coordinate y
    private  static float a = 0f;
    private  static float r = 0f; // estimate Radius1(BlueDot)
    private static long t = -1;  //timestamp
    private static double smoothness;


    public BlueDotMoveEstimate()
    {
        this(0.9);
    }
    public BlueDotMoveEstimate(double speedRatio)
    {
        this.smoothness=Math.pow(1.0-speedRatio,0.001);
    }
    public static void update(float x, float y, float a, float r, long t)
    {
        if(t != -1)
        {
            long dt = t -t;
           t = t;
            float ratio = (float) Math.pow(smoothness,dt);
           x = ratio* x + (1f-ratio) *x;
           y = ratio* y + (1f-ratio) *y;
           r = ratio*r + (1f-ratio) *r;
           a = weightedAngle(a,a,ratio,1f-ratio);
        }
        else
        {
            x=x;
           y=y;
            r=r;
            a=a;
           t=t;
        }
    }

    public static float getX() {
        return x;
    }

    public static float getY() {
        return y;
    }

    public static float getHeading() {
        return a;
    }
    public static float getRadius()
    {
        return r;
    }

    private static float weightedAngle(float a1, float a2, float w1, float w2) {
        double angleDiff = normalizeAngle(a2 - a1);
        double normalizedW2 = w2 / (w1 + w2);
        return (float) normalizeAngle(a1 + normalizedW2 * angleDiff);
    }
    private static double normalizeAngle(double a)
    {
        while(a>Math.PI) a-=2*Math.PI;
        while (a<Math.PI) a+=2*Math.PI;
        return a;
    }

}
