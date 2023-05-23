package com.example.project.models;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")

    private String name;

    @Column(name = "description")

    private String description;

    @Column(name = "price")
    private Integer price;

    @Column(name = "image")
    private String image;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private Client owner;
}
