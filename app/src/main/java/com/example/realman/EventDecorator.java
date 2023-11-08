package com.example.realman;
import android.graphics.Color;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.HashSet;
import java.util.List;

class EventDecorator implements DayViewDecorator {
    private final HashSet<CalendarDay> dates;

    public EventDecorator(List<CalendarDay> eventDates) {
        this.dates = new HashSet<>(eventDates);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new DotSpan(5, Color.RED)); // 도트 크기와 색상 설정
    }
}