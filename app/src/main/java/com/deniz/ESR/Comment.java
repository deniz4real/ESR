package com.deniz.ESR;

public class Comment {
    private String text;
    private String date;
    private String elevator;
    private String ramp;
    private String photo1Path;
    private String photo2Path;
    private String userName;
private int rating;
    public Comment(String text, String date, String elevator, String ramp,String photo1Path,String photo2Path,String userName,int rating) {
        this.text = text;
        this.date = date;
        this.elevator = elevator;
        this.ramp = ramp;
        this.photo1Path = photo1Path;
        this.photo2Path = photo2Path;
        this.userName = userName;
        this.rating=rating;
    }

    public int getRating() {
        return rating;
    }
    public String getUserName() {
        return userName;
    }

    public String getText() {
        return text;
    }

    public String getDate() {
        return date;
    }

    public String getElevator() {
        return elevator;
    }

    public String getRamp() {
        return ramp;
    }
    public String getPhoto1Path() {
        return photo1Path;
    }

    public String getPhoto2Path() {
        return photo2Path;
    }
}

