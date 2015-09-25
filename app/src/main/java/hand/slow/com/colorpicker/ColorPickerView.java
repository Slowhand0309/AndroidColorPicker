package hand.slow.com.colorpicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * ColorPickerView of HSV models
 *
 * @version 1.0
 * @author slowhand
 */
public class ColorPickerView extends View {

    /**  分割数 */
    private static final int DIVISION = 10;

    /** 円環描画のストローク幅 */
    private static final int SWEEP_STROKE_WIDTH = 50;

    /** 矩形描画のストローク幅 */
    private static final int LINEAR_STROKE_WIDTH = 2;

    /** √2 */
    private static final float SQRT2 = (float) Math.sqrt(2.0);

    /** 選択色(均等に分散する) */
    private final int[] mSelectColors =
            {
                    0xFFFF0000,
                    0xFFFF00FF,
                    0xFF0000FF,
                    0xFF00FFFF,
                    0xFF00FF00,
                    0xFFFFFF00,
                    0xFFFF0000
            };

    /** 選択彩度(均等に分散する) */
    private int[] mSelectChromas = new int[DIVISION];
    /*
        {
            0xFF000000,
            0xFF888888,
            0xFFFFFFFF
        };
    */

    // 円の中心位置
    private int mCenterX;
    private int mCenterY;

    private RectF mOvalRect;
    private Paint mSweepPaint;
    private Paint mLinearPaint;
    private Shader mSweepGradient;
    private Shader mLinearGradient;
    private int mSelectedColor;
    private float mSelectedHue = 0.0f;

    /**
     * コンストラクタ
     *
     * @param context コンテキスト
     */
    public ColorPickerView(Context context) {
        this(context, null, 0);
    }

    /**
     * コンストラクタ
     *
     * @param context コンテキスト
     * @param attrs 属性
     */
    public ColorPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * コンストラクタ
     *
     * @param context コンテキスト
     * @param attrs 属性
     * @param defStyle デフォルトスタイル値
     */
    public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // 円環描画用グラデーションの作成
        mSweepGradient = new SweepGradient(0, 0, mSelectColors, null);

        // 円環描画用ペイント
        mSweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSweepPaint.setStyle(Style.STROKE);
        mSweepPaint.setShader(mSweepGradient);
        mSweepPaint.setStrokeWidth(SWEEP_STROKE_WIDTH);

        // 矩形描画用ペイント
        mLinearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinearPaint.setStyle(Style.FILL);
        mLinearPaint.setStrokeWidth(LINEAR_STROKE_WIDTH);

    }

    /**
     * 色(argb)設定
     *
     * @param color 色(argb)
     */
    public void setColor(int color) {
        mSelectedColor = color;
    }

    /**
     * 描画処理
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mCenterX = canvas.getWidth() / 2;
        mCenterY = canvas.getHeight() / 2;

        // 円環の描画位置を算出
        if (mOvalRect == null) {
            float r = getStrokeRadius(mCenterX, mSweepPaint);
            mOvalRect = new RectF(-r, -r, r, r);
        }

        // 円環の色選択を描画
        canvas.translate(mCenterX, mCenterY);
        canvas.drawOval(mOvalRect, mSweepPaint);

        // 彩度/明度選択エリアを描画
        drawSvArea(canvas, getRadius(mCenterX, mSweepPaint));
    }

    /**
     * 彩度/明度選択エリアの描画
     *
     * @param canvas キャンバス
     */
    private void drawSvArea(Canvas canvas, float r) {

        // 円に内接する正方形の一辺
        float side = r * SQRT2;
        float divSide = side / 2.0f;

        for (float y = 0.0f; y < side; y += 0.1f) {

            int i = 0;
            for (float x = 0.0f; i < DIVISION; x += 0.1f, i++) {
                mSelectChromas[i] = getHsvColor(mSelectedHue, x, y * (side / 100.0f));
            }

            mLinearGradient = new LinearGradient(-divSide, 0, divSide, 0,
                    mSelectChromas, null, Shader.TileMode.CLAMP);
            mLinearPaint.setShader(mLinearGradient);

            // 線形上に描画
            canvas.drawLine(-divSide, y - divSide, divSide, y - divSide, mLinearPaint);
        }
    }

    /**
     * HSV値からARGB値に変換します
     *
     * @param hue 色相
     * @param saturation 彩度
     * @param value 明度
     * @return argb
     */
    private int getHsvColor(float hue, float saturation, float value) {
        float[] hsv = new float[3];

        if (hue >= 360.0f) {
            hue = 359.0f;
        } else if (hue < 0.0f) {
            hue = 0.0f;
        }

        if (saturation > 1.0f) {
            saturation = 1.0f;
        } else if (saturation < 0.0f) {
            saturation = 0.0f;
        }

        if (value > 1.0f) {
            value = 1.0f;
        } else if (value < 0.0f) {
            value = 0.0f;
        }

        hsv[0] = hue;
        hsv[1] = saturation;
        hsv[2] = value;
        return Color.HSVToColor(hsv);
    }

    /**
     * ストローク時に指定する円の半径を取得
     *
     * @param cx 円の中心位置
     * @param paint ストロークするペイント
     * @return 半径
     */
    private float getStrokeRadius(int cx, Paint paint) {
        return cx - paint.getStrokeWidth() * 0.5f;
    }

    /**
     * ストローク部を省いた半径を取得
     *
     * @param cx 円の中心位置
     * @param paint ペイントクラス
     * @return 半径
     */
    private float getRadius(int cx, Paint paint) {
        return cx - paint.getStrokeWidth();
    }

    /**
     * タッチイベント
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        boolean isTouchOval = false;
        boolean isTouchRect = false;

        float r = getRadius(mCenterX, mSweepPaint);
        if (isTouchCircle(mCenterX, mCenterY, x, y, mCenterX)
                && !isTouchCircle(mCenterX, mCenterY, x, y, r)) {
            // 円環内にタッチ
            isTouchOval = true;
        }

        float divSide = (r * SQRT2) / 2.0f;
        if (x >= mCenterX - divSide && x < mCenterX + divSide
                && y >= mCenterY - divSide && y < mCenterY + divSide) {
            // 矩形内にタッチ
            isTouchRect = true;
        }

        if (isTouchOval) {
            // 円環内にタッチ
            float angle = (float) Math.atan2((y - mCenterY), (x - mCenterX));
            float unit = (float) (angle / (Math.PI * 2.0f));
            if (unit < 0.0f) {
                unit += 1.0f;
            }
            // 色を算出
            mSelectedColor = interpColor(mSelectColors, unit);
            mSelectedHue = getHue(mSelectedColor);
            invalidate();
        }

        if (isTouchRect) {
            // 矩形内にタッチ
            float tx = mCenterX - divSide;
            float ty = mCenterY - divSide;
            mSelectedColor = getHsvColor(mSelectedHue, (x - tx) / 100.0f, ((r * SQRT2) - (y - ty)) / 100.0f);
            invalidate();

            // TODO for debug
            Toast.makeText(getContext(), "TouchRect " + mSelectedColor, Toast.LENGTH_SHORT).show();
            // TODO for debug
        }

        return true;
    }

    /**
     * 円との当たり判定
     *
     * @param cx 中心X位置
     * @param cy 中心Y位置
     * @param px タッチ位置X
     * @param py タッチ位置Y
     * @param r 半径
     * @return true : 当たり / false : 当たりでない
     */
    private boolean isTouchCircle(
            float cx, float cy, float px, float py, float r) {

        float tx = px - cx;
        float ty = py - cy;

        float tr = (float) Math.sqrt(tx * tx + ty * ty);
        return r >= tr;
    }

    /**
     * 選択位置から色を取得
     *
     * @param colors 色分散値
     * @param unit 選択位置
     * @return 色(argb)
     */
    private int interpColor(int colors[], float unit) {

        if (unit <= 0.0f) {
            return colors[0];
        }
        if (unit >= 1.0f) {
            return colors[colors.length - 1];
        }

        float p = unit * (colors.length - 1);
        int i = (int)p;
        p -= i;

        // now p is just the fractional part [0...1) and i is the index
        int c0 = colors[i];
        int c1 = colors[i + 1];
        int a = ave(Color.alpha(c0), Color.alpha(c1), p);
        int r = ave(Color.red(c0),   Color.red(c1),   p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0),  Color.blue(c1),  p);

        return Color.argb(a, r, g, b);
    }

    /**
     * 色相取得
     *
     * @param color 色(argb)
     * @return 色相
     */
    private float getHue(int color) {
        float hsv[] = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv[0];
    }

    /**
     *
     * @param s
     * @param d
     * @param p
     * @return
     */
    private int ave(int s, int d, float p) {
        return s + Math.round(p * (d - s));
    }
}

