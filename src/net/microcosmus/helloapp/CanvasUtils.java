package net.microcosmus.helloapp;


import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

public class CanvasUtils {

    public static void buildDiscountRateIcon(ImageView img, int rate, int width) {
        img.setBackgroundColor(Color.TRANSPARENT);

        int height = width;
        int x = width / 2;
        int y = height / 2;
        int rad1 = width / 2;
        int rad2 = width / 2 - 2;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        Paint p0 = new Paint();
        p0.setARGB(256, 255, 255, 255);
        RectF rectF = new RectF();
        rectF.set(0,0, width, height);
        c.drawRoundRect(rectF, 0, 0, p0);

        Paint p1 = new Paint();
        p1.setColor(Color.parseColor("#fffffd"));
        p1.setAntiAlias(true);
        c.drawCircle(x, y, rad1, p1);

        Paint p2 = new Paint();
        p2.setColor(Color.parseColor("#DD3E53"));
        p2.setAntiAlias(true);
        c.drawCircle(x, y, rad2, p2);

        float textHeight = 16;
        p1.setTextSize(textHeight);
        p1.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        String rateText = rate + "%";
        float textWidth = p1.measureText(rateText);
        c.drawText(rateText, x - textWidth / 2, y + 6, p1);

        img.setBackgroundDrawable(new BitmapDrawable(bmp));
    }

    public static void buildBentEdge(ImageView img, int edge) {
        int width = 24;
        int height = 24;


        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        Paint p0 = new Paint();
        p0.setARGB(256, 255, 255, 255);
        RectF rectF = new RectF();
        rectF.set(0,0, width, height);
        c.drawRoundRect(rectF, 0, 0, p0);

        Paint p1 = new Paint();
        p1.setColor(Color.parseColor("#565656"));
        p1.setAntiAlias(true);
        p1.setStyle(Paint.Style.FILL);
        Path path = new Path();
        path.moveTo(width, height);
        path.lineTo(width - edge, height);
        path.lineTo(width, width - edge);
        path.lineTo(width, height);
        path.close();
        c.drawPath(path, p1);

        img.setBackgroundDrawable(new BitmapDrawable(bmp));
    }


    public static void buildBtnEdge(ImageView img, int width, int height) {


        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        Paint p0 = new Paint();
        p0.setARGB(256, 255, 255, 255);
        RectF rectF = new RectF();
        rectF.set(0,0, width, height);
        c.drawRoundRect(rectF, 0, 0, p0);

        Paint p1 = new Paint();
        p1.setColor(Color.parseColor("#00A1D7"));
        p1.setAntiAlias(true);
        p1.setStyle(Paint.Style.FILL);

        Path path = new Path();
        path.moveTo(0, 0);
        path.lineTo(width, height / 2);
        path.lineTo(0, height);
        path.lineTo(0, 0);
        path.close();
        c.drawPath(path, p1);

        img.setBackgroundDrawable(new BitmapDrawable(bmp));
    }
}
