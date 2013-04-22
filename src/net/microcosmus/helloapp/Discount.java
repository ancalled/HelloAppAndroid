package net.microcosmus.helloapp;

import android.os.Parcelable;

import java.io.Serializable;
import java.util.Date;

public class Discount implements Serializable {

    private Long id;
    private String title;
    private String place;
    private Integer rate;
    private Date goodThrough;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public Integer getRate() {
        return rate;
    }

    public void setRate(Integer rate) {
        this.rate = rate;
    }

    public Date getGoodThrough() {
        return goodThrough;
    }

    public void setGoodThrough(Date goodThrough) {
        this.goodThrough = goodThrough;
    }


}
