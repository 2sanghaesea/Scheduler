package com.example.realman;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

public class TodayColorDecorator implements DayViewDecorator {
    private final int pinkColor = Color.parseColor("#FFC0CB");

    private final CalendarDay selectedDate;

    public TodayColorDecorator(CalendarDay selectedDate) {
        this.selectedDate = selectedDate;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return selectedDate != null && selectedDate.equals(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setSelectionDrawable(new ColorDrawable(pinkColor));
    }
}