package com.ritu.upgrade.widget.span;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;

/**
 * @author ritu on 15-Jun-18
 * */
public class SpannableTextView extends AppCompatTextView {

    public SpannableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpannableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTextPair(SpannablePair... pairs){
        if (pairs == null || pairs.length == 0){
            return;
        }
        String text = "";
        StringBuilder builder = new StringBuilder();
        for (SpannablePair pair : pairs){
            if (!TextUtils.isEmpty(pair.getText())){
                builder.append(pair.getText());
            }
        }
        text = builder.toString();
        SpannableStringBuilder spannableString = new SpannableStringBuilder(text);
        int startPosition = 0;
        int endPosition;
        for (SpannablePair pair : pairs){
            if (!TextUtils.isEmpty(pair.getText())){
                startPosition += startPosition== 0?0:1;
                endPosition = startPosition + pair.getText().length();
                if (pair.getListener() != null){
                    spannableString.setSpan(pair.getListener(),startPosition, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                startPosition += pair.getText().length();
            }
        }
        setText(spannableString);
        setMovementMethod(LinkMovementMethod.getInstance());
    }
}
