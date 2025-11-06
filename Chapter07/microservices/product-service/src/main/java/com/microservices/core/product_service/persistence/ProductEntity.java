package com.microservices.core.product_service.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The @Document(collection = "products") annotation is used to mark the class as an entity
 * class used for MongoDB, that is, mapped to a collection in MongoDB with the name products
 *
 * The id field is used to hold the database identity of each stored entity, corresponding to the primary
 * key when using a relational database. We will delegate the responsibility of generating unique values
 * for the id field to Spring Data
 *
 * The version field is used to implement optimistic locking, allowing Spring Data to verify that updates of
 * an entity in the database do not overwrite a concurrent update. If the value of the version field stored
 * in the database is higher than the value of the version field in an update request, this indicates that
 * the update is performed on stale data—the information to be updated has been updated by someone
 * else since it was read from the database
 */
@Document(collection = "products")
public class ProductEntity {

    @Id
    private String id;

    @Version
    private Integer version;

    // The @Indexed(unique = true) annotation is used to get a unique index created for the business key, productId.
    @Indexed(unique = true)
    private int productId;

    private String name;
    private int weight;

    public ProductEntity() {
    }

    public ProductEntity(int productId, String name, int weight) {
        this.productId = productId;
        this.name = name;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
