package com.example.tasksgroups.data;

import android.content.Context;

import com.example.tasksgroups.R;

import java.util.ArrayList;
import java.util.List;

public class DummyData {

    public static List<Task> getDummyTaskList(Context context, int size){
        ArrayList<Task> lista = new ArrayList<Task>();
        if(size>0){
            for(int i = 0; i<size; i++){
                String[] prioridades = context.getResources().getStringArray(R.array.priority_array);
                String[] states = context.getResources().getStringArray(R.array.estate_array);
                Task task = new Task("id"+i,
                        "name"+i,
                        "description"+i,
                        prioridades[i%prioridades.length],
                        states[i%states.length] );
                lista.add(task);
            }
        }
        return lista;
    }


    public static List<User> getDummyUserList(Context context, int size){
        ArrayList<User> lista = new ArrayList<User>();
        if(size>0){
            for(int i = 0; i<size; i++){
                User user = new User("id"+i, "name"+i,"draw"+i);
                lista.add(user);
            }
        }
        return lista;
    }

    public static List<Group> getDummyGroupList(Context context, int size){
        ArrayList<Group> lista = new ArrayList<Group>();
        if(size>0){
            for(int i = 0; i< size; i++){
                lista.add(new Group("id"+i, "name"+i, "description"+i ,""));
            }
        }
        return lista;
    }

    public static List<Task> getDummyRandomTaskList(Context context, int size){
        ArrayList<Task> lista = new ArrayList<Task>();
        if(size>0){
            for(int i = 0; i<size; i++){
                String[] prioridades = context.getResources().getStringArray(R.array.priority_array);
                String[] states = context.getResources().getStringArray(R.array.estate_array);
                Task task = new Task("id"+randomInt(100),
                        "name"+randomInt(100),
                        "description"+i,
                        prioridades[i%prioridades.length],
                        states[i%states.length] );
                lista.add(task);
            }
        }
        return lista;

    }



    public static List<Group> getDummyRandomGroupList(Context context, int size){
        ArrayList<Group> lista = new ArrayList<Group>();
        if(size>0){
            for(int i = 0; i< size; i++){
                lista.add(new Group("id"+randomInt(100), "name"+randomInt(100), "description"+i ,""));
            }
        }
        return lista;
    }

    public static List<AddedFile> getDummyAddedFileList(Context context, int size){
        ArrayList<AddedFile> lista = new ArrayList<AddedFile>();
        if(size>0){
            for(int i = 0; i<size; i++){
                AddedFile file = new AddedFile("id"+i, "name"+i,"url"+i);
                lista.add(file);
            }
        }
        return lista;
    }

    public static int randomInt(int max){
        int i = (int) (Math.random()*max);
      //  Log.e("random ", "random = "+i);
        return i;
    }
}
