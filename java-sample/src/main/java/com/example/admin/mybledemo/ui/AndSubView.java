package com.example.admin.mybledemo.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.admin.mybledemo.R;

/**
 * TODO: document your custom view class.
 */
public class AndSubView extends LinearLayout {
    /** 显示文本 */
    private TextView mTextView;
    /** 增加按钮 */
    private Button btAdd;
    /** 减少按钮 */
    private Button btReduce;
    /** 显示文本的长宽 */
    private int textFrameWidth;
    /** 显示文本及button中文字的颜色 */
    private int textColor;
    /** 初始值 */
    private int initValue;
    /** 最大值 */
    private int maxValue;
    /** 最小值 */
    private int minValue;
    /** 显示文本及button中文字的大小 */
    private int textSize;
    /** 显示文本的背景 */
    private Drawable textFrameBackground;
    /** 增加按钮的背景 */
    private Drawable addBackground;
    /** 减少按钮的背景 */
    private Drawable subBackground;
    /** 增加按钮的大小 */
    private int addWidth;
    /** 减少按钮的大小 */
    private int subWidth;
    /** 增加按钮中的文本 */
    private String addText;
    /** 减少按钮中的文本 */
    private String subText;

    public AndSubView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWidget(context);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AndSubView);
        textColor = a.getColor(R.styleable.AndSubView_textColor, getResources().getColor(android.R.color.black));
        textSize = a.getDimensionPixelSize(R.styleable.AndSubView_textSize, 16);
        textFrameBackground =  a.getDrawable(R.styleable.AndSubView_textFrameBackground);
        textFrameWidth = a.getDimensionPixelSize(R.styleable.AndSubView_textFrameWidth, 48);
        addBackground = a.getDrawable(R.styleable.AndSubView_addBackground);
        subBackground = a.getDrawable(R.styleable.AndSubView_subBackground);
        initValue = a.getInt(R.styleable.AndSubView_initValue, 0);
        maxValue = a.getInt(R.styleable.AndSubView_maxValue, 1000000000);
        minValue = a.getInt(R.styleable.AndSubView_minValue, -1000000000);
        addWidth = a.getDimensionPixelSize(R.styleable.AndSubView_addWidth, 48);
        subWidth = a.getDimensionPixelSize(R.styleable.AndSubView_subWidth, 48);
        addText = a.getString(R.styleable.AndSubView_addText);
        subText = a.getString(R.styleable.AndSubView_subText);
        setAddBackground(addBackground);
        setAddText(addText);
        setAddWidth(addWidth);
        setInitValue(initValue);
        setMaxValue(maxValue);
        setMinValue(minValue);
        setSubBackground(subBackground);
        setSubText(subText);
        setSubWidth(subWidth);
        setTextColor(textColor);
        setTextFrameBackground(textFrameBackground);
        setTextFrameWidth(textFrameWidth);
        setTextSize(textSize);
        a.recycle();
    }


    protected void onFinishInflate() {
        super.onFinishInflate();
        addListener();

    }

    public void initWidget(Context context){
        LayoutInflater.from(context).inflate(R.layout.sample_and_sub_view, this);
        mTextView = (TextView)findViewById(R.id.et01);
        btAdd = (Button)findViewById(R.id.bt01);
        btReduce = (Button)findViewById(R.id.bt02);
    }

    public void addListener(){
        btAdd.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {

                int num = Integer.valueOf(mTextView.getText().toString());
                num++;
                if (num >= maxValue+1)
                    return;
                mTextView.setText(Integer.toString(num));
            }
        });

        btReduce.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                int num = Integer.valueOf(mTextView.getText().toString());
                num--;
                if (num <= minValue-1)
                    return;
                mTextView.setText(Integer.toString(num));
            }
        });
    }


    public int getTextFrameWidth() {
        return textFrameWidth;
    }


    public void setTextFrameWidth(int textFrameWidth) {
        this.textFrameWidth = textFrameWidth;
        mTextView.setWidth(textFrameWidth);
        mTextView.setHeight(textFrameWidth);
    }


    public int getTextColor() {
        return textColor;
    }


    public void setTextColor(int textColor) {
        this.textColor = textColor;
        mTextView.setTextColor(textColor);
        btAdd.setTextColor(textColor);
        btReduce.setTextColor(textColor);
    }


    public int getInitValue() {
        return initValue;
    }


    public void setInitValue(int initValue) {
        this.initValue = initValue;
        mTextView.setText(String.valueOf(initValue));
    }

    public int getValue() {
        return Integer.valueOf(mTextView.getText().toString());
    }


    public int getMaxValue() {
        return maxValue;
    }


    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }


    public int getMinValue() {
        return minValue;
    }


    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }


    public int getTextSize() {
        return textSize;
    }


    public void setTextSize(int textSize) {
        this.textSize = textSize;
        mTextView.setTextSize(textSize);
    }


    public Drawable getTextFrameBackground() {
        return textFrameBackground;
    }


    public void setTextFrameBackground(Drawable textFrameBackground) {
        this.textFrameBackground = textFrameBackground;
        mTextView.setBackgroundDrawable(textFrameBackground);
    }


    public Drawable getAddBackground() {
        return addBackground;
    }


    public void setAddBackground(Drawable addBackground) {
        this.addBackground = addBackground;
        Resources res = getResources();
        int color = res.getColor(android.R.color.darker_gray);
        Drawable drawable = new ColorDrawable(color);
        btAdd.setBackgroundDrawable(addBackground==null?drawable:addBackground);
    }


    public Drawable getSubBackground() {
        return subBackground;
    }


    public void setSubBackground(Drawable subBackground) {
        this.subBackground = subBackground;
        Resources res = getResources();
        int color = res.getColor(android.R.color.darker_gray);
        Drawable drawable = new ColorDrawable(color);
        btReduce.setBackgroundDrawable(subBackground==null?drawable:subBackground);
    }


    public int getAddWidth() {
        return addWidth;
    }


    public void setAddWidth(int addWidth) {
        this.addWidth = addWidth;
        btAdd.setWidth(addWidth);
        btAdd.setHeight(addWidth);
    }


    public int getSubWidth() {
        return subWidth;
    }


    public void setSubWidth(int subWidth) {
        this.subWidth = subWidth;
        btReduce.setWidth(subWidth);
        btReduce.setHeight(subWidth);
    }


    public String getAddText() {
        return addText;
    }


    public void setAddText(String addText) {
        this.addText = addText;
        btAdd.setText(addText);
    }


    public String getSubText() {
        return subText;
    }


    public void setSubText(String subText) {
        this.subText = subText;
        btReduce.setText(subText);
    }
}
