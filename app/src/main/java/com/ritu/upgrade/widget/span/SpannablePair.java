package com.ritu.upgrade.widget.span;

import android.text.style.ClickableSpan;

public class SpannablePair {
    private String text;
    private ClickableSpan listener;


    public SpannablePair(String text,  ClickableSpan listener) {
        this.text = text;
        this.listener = listener;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ClickableSpan getListener() {
        return listener;
    }

    public void setListener(ClickableSpan listener) {
        this.listener = listener;
    }
}
