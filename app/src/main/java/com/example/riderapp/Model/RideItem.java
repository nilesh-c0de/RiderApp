package com.example.riderapp.Model;

public class RideItem {

    private String text_date;
    private String text_pickup;
    private String text_drop_off;


    public RideItem(String text_date, String text_pickup, String text_drop_off) {
        this.text_date = text_date;
        this.text_pickup = text_pickup;
        this.text_drop_off = text_drop_off;
    }

    public String getText_date() {
        return text_date;
    }

    public void setText_date(String text_date) {
        this.text_date = text_date;
    }

    public RideItem() {
    }

    public String getText_pickup() {
        return text_pickup;
    }

    public void setText_pickup(String text_pickup) {
        this.text_pickup = text_pickup;
    }

    public String getText_drop_off() {
        return text_drop_off;
    }

    public void setText_drop_off(String text_drop_off) {
        this.text_drop_off = text_drop_off;
    }
}
