package com.example.tasksgroups.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {


    @PrimaryKey
    @NonNull
    String id;
    String name;
    String drawable;


    @Ignore
    public User() {
    }

    public User(@NonNull String id, String name, String drawable) {
        this.id = id;
        this.name = name;
        this.drawable = drawable;
    }

    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDrawable() {
        return drawable;
    }

    public void setDrawable(String drawable) {
        this.drawable = drawable;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", drawable='" + drawable + '\'' +
                '}';
    }
}
