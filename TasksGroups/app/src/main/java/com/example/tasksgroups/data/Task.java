package com.example.tasksgroups.data;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String description;
    private String priority;
    private String state;

    @Ignore
    public Task() {
    }

    public Task(@NonNull String id, String name, String description, String priority, String state ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.state = state;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", priority=" + priority +
                ", state=" + state +
                '}';
    }
}
