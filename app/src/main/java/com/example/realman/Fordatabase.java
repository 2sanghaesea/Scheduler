package com.example.realman;
//firebase로 데이터를 보내기 위한 클래스
public class Fordatabase {
    String day;
    String priority;
    String todo;

    public Fordatabase(){
    }

    public String getday(){
        return day;
    }
    public void setday(){
        this.day = day;
    }

    public String getPriority(){
        return priority;
    }

    public void setPriority(){
        this.priority = priority;
    }


    public String getTodo(){
        return todo;
    }

    public Fordatabase(String day, String todo, String priority){
        this.day = day;
        this.todo = todo;
        this.priority=priority;
    }
}
