package com.example.apartmentfinder.model;

public class Apartment {
    private String title;
    private String price;
    private String link;

    public Apartment(String title, String price, String link) {
        this.title = title;
        this.price = price;
        this.link = link;
    }

    public String getTitle() { return title; }
    public String getPrice() { return price; }
    public String getLink() { return link; }
}

