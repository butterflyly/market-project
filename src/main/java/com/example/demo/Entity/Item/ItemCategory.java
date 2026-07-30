package com.example.demo.Entity.Item;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 디지털기기, 생활가전 등

    public ItemCategory(String name) {
        this.name = name;
    }

    public static ItemCategory of(String name) {
        return new ItemCategory(name);
    }

    public void rename(String newName) {
        this.name = newName;
    }
}
