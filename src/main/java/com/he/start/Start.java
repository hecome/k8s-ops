package com.he.start;

import com.he.server.Core;

public class Start {

    public static void main(String[] args) {
        try {
            while (true){
                Core app=new Core();
                app.listContainers();
                Thread.sleep(1000L*60*30);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
