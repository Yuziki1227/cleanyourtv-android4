package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The clock and date, as a TextView that keeps itself current. android.widget.TextClock is API 17, so the
 * launcher's layouts use this instead and keep the same formats. Updated by ACTION_TIME_TICK (every minute),
 * ACTION_TIME_CHANGED and ACTION_TIMEZONE_CHANGED. An empty format hides the view's text, as TextClock does.
 */
public final class ClockText extends TextView {

    private final BroadcastReceiver ticker = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };
    private String format12;
    private String format24;

    public ClockText(Context context) {
        super(context);
    }

    public ClockText(Context context, AttributeSet attrs) {
        super(context, attrs);
        readFormats(context, attrs);
    }

    public ClockText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readFormats(context, attrs);
    }

    private void readFormats(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ClockText);
        try {
            format12 = a.getString(R.styleable.ClockText_clockFormat12);
            format24 = a.getString(R.styleable.ClockText_clockFormat24);
        } finally {
            a.recycle();
        }
    }

    private void update() {
        boolean is24 = DateFormat.is24HourFormat(getContext());
        String format = is24 ? format24 : format12;
        if (format == null) {
            java.text.DateFormat fallback = DateFormat.getTimeFormat(getContext());
            setText(fallback.format(new Date()));
            return;
        }
        if (format.isEmpty()) {
            setText("");
            return;
        }
        try {
            setText(new SimpleDateFormat(format, Locale.getDefault()).format(new Date()));
        } catch (IllegalArgumentException e) {
            // A malformed format from a theme: leave the previous text rather than take Home down.
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(ticker, filter);
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            getContext().unregisterReceiver(ticker);
        } catch (RuntimeException ignored) {
            // Not registered: a detach after the process was already torn down.
        }
        super.onDetachedFromWindow();
    }
}
