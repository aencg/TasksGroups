package com.example.tasksgroups.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "groups")
public class Group {
    @Ignore
    public Group() {
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

    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String description;
    private String adminId;
    public Group(@NonNull String id, String name, String description, String adminId ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.adminId = adminId;
    }



    @Override
    public String toString() {
        return "Group{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description +
                ", adminId='" + adminId +
                '}';
    }



    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("description", description);
        result.put("adminId", adminId);
        return result;
    }
}
