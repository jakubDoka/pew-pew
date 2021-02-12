package pew;

import arc.util.*;
import mindustry.content.*;
import mindustry.mod.*;
import pew.Shooter.*;

import java.io.*;
import java.lang.reflect.*;

import static pew.Shooter.Util.load;

public class Main extends Plugin {
    public static Shooter shooter = new Shooter();

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("pew-content", "<units/bullets/items>", "shows all possible options for game content relevant to plugin config", (args) -> {
            switch(args[0]){
                case "units":
                    Log.info(listFields(UnitTypes.class));
                    break;
                case "bullets":
                    Log.info(listFields(Bullets.class));
                    break;
                case "items":
                    Log.info(listFields(Items.class));
                    break;
                default:
                    Log.info("wrong option");
                    ;
            }
        });

        handler.register("pew-load", "reloads configuration", (args) -> {
            shooter.load();
            Log.info("loaded");
        });
    }

    public static <T> String listFields(Class<T> c) {
        StringBuilder sb = new StringBuilder();
        for (Field f : c.getFields()) {
            sb.append(f.getName()).append(" ");
        }
        return  sb.toString();
    }
}
