package com.github.bqbs.heartbeat4u

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * 心跳不
 */
class HeartBeat4UView : View {


    private lateinit var mPaint: Paint
    private var textWidth: Float = 0f
    private var textHeight: Float = 0f


    val random = Random(System.currentTimeMillis())


    var centerX = 0f
    var centerY = 0f


    var points: ArrayList<PointF> = ArrayList()
    var extraPoints: ArrayList<PointF> = ArrayList()
    var innerPoints: ArrayList<PointF> = ArrayList()
    val allPoints = MutableList<ArrayList<Pair<PointF, Int>>>(20) { ArrayList() }


    var frame = 0

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.HeartBeat4UView, defStyle, 0
        )

        a.recycle()

        mPaint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
        }

        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        centerX = (measuredWidth / 2).toFloat()
        centerY = (measuredHeight / 2).toFloat()



        // 初始化点
        List(500) {
            heartBeat(random.nextFloat() * 2 * PI)
        }.forEach {
            points.add(it)
            for (i in 0 until 3) {
                extraPoints.add(scatterInner(it, 0.1f))
            }
            innerPoints.add(scatterInner(it, 0.3f))

        }


        //算20帧
        for (i in 0 until 20) {
            calc(i)
        }

        Log.d(TAG, "onMeasure: $centerX $centerY")
    }


    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)

        Log.d(
            TAG,
            "onDraw: $frame"
        )
        allPoints[frame].forEach {
            mPaint.color = Color.parseColor("#ffff7171")
            mPaint.strokeWidth = it.second.toFloat()
            canvas.drawPoint(
                it.first.x,
                it.first.y,
                mPaint
            )
        }
        frame++
        frame %= 20

        postInvalidateDelayed(50)
    }


    private fun heartBeat(t: Float, enlarge: Int = IMAGE_ENLARGE): PointF {

        var x = 16 * (sin(t).pow(3))
        var y = -(13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t))
        x *= enlarge
        y *= enlarge
        x += centerX
        y += centerY
        Log.d(TAG, "MyheartBeat: t=$t x=$x y = $y width=$width height=$height $centerX $centerY")

        return PointF(x, y)
    }


    /**
     * 计算位置
     */
    private fun calcPosition(origin: PointF, ratio: Float): PointF {
        val x = origin.x
        val y = origin.y

        //这是魔法
        val force = 1.0 / (((x - centerX).pow(2) + (y - centerY).pow(2)).pow(0.520f))

        val dx = ratio * force * (x - centerX) + random.nextInt(-1, 1)
        val dy = ratio * force * (y - centerY) + random.nextInt(-1, 1)
        Log.d(TAG, "calcPosition: $ratio $force $x, $y    $dx,$dy $centerX, $centerY")
        return PointF(x - dx.toFloat(), y - dy.toFloat())
    }

    private fun scatterInner(pointF: PointF, beta: Float = 0.15f): PointF {

        val ratioX = -beta * ln(random.nextFloat())
        val ratioY = -beta * ln(random.nextFloat())
        val dx = ratioX * (pointF.x - centerX)
        val dy = ratioY * (pointF.y - centerY)

        Log.d(TAG, "scatterInner: $pointF $ratioX $ratioY $dx $dy")
        return PointF(
            pointF.x - dx,
            pointF.y - dy
        )
    }

    private fun shrink(pointF: PointF, ratio: Float): PointF {
        val x = pointF.x
        val y = pointF.y
        val force = 1 / (((x - centerX).pow(2) + (y - centerY).pow(2)).pow(0.520f))
        val dx = ratio * force * (x - centerX) + random.nextInt(-1, 1)
        val dy = ratio * force * (y - centerY) + random.nextInt(-1, 1)
        return PointF(x - dx, y - dy)
    }

    private fun calc(frame: Int) {
        // 这个地方使用sin的时候好像不会正常跳动
        val ratio = 10 * cos(frame / 10 * PI)
        val list = ArrayList<Pair<PointF, Int>>()

        points.forEach {
            val np = calcPosition(it, ratio)
            val pp = list.find {
                it.first.x == np.x && it.first.y == np.y
            }
            if (pp == null) {
                list.add(Pair(np, random.nextInt(1, 3)))
            }
        }
        extraPoints.forEach {
            val np = calcPosition(it, ratio)
            val pp = list.find {
                it.first.x == np.x && it.first.y == np.y
            }
            if (pp == null) {
                list.add(Pair(np, random.nextInt(1, 2)))
            }
        }

        innerPoints.forEach {
            val np = calcPosition(it, ratio)
            val pp = list.find {
                it.first.x == np.x && it.first.y == np.y
            }
            if (pp == null) {
                list.add(Pair(np, random.nextInt(1, 2)))
            }

        }

        // 生成外围光环  这个数量请自行斟酌一下
        for (i in 0..1000) {
            val p = heartBeat(random.nextFloat() * 2 * PI)
            val p2 = shrink(pointF = p, ratio)
            val pp = list.find {
                it.first.x == p2.x && it.first.y == p2.y
            }
            if (pp == null) {
                list.add(Pair(p2.also {
                    it.x += random.nextInt(-14, 14)
                    it.y += random.nextInt(-14, 14)
                }, random.nextInt(1, 2)))
            }
        }

        allPoints[frame] = list

    }

    companion object {
        const val TAG = "HeartBeat4u"

        /**
         * 放大比例
         */
        const val IMAGE_ENLARGE = 20

        /**
         *
         */
        const val PI = 3.14f


    }
}
