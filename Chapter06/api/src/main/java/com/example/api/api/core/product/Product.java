package com.example.api.api.core.product;

/**
 * This type of POJO class is also known as a Data Transfer Object (DTO) as it is used to transfer
 * data between the API implementation and the caller of the API.
 */

public class Product {

    private int productId;
    private String name;
    private int weight;
    private String serviceAddress;

    // constructor to initialize
    public Product() {
        productId = 0;
        name = null;
        weight = 0;
        serviceAddress = null;
    }

    public Product(int productId, String name, int weight, String serviceAddress) {
        this.productId = productId;
        this.name = name;
        this.weight = weight;
        this.serviceAddress = serviceAddress;
    }

    public int getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public String getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }
}
