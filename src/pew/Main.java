package pew;

import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.type.Weapon;
import pew.Shooter.*;
import pew.Shooter.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import static pew.Shooter.Util.load;

public class Main extends Plugin {
    public static Shooter shooter;
    public static Boolean debug = false;

    @Override
    public void init(){
        shooter = new Shooter();
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("pew-content", "<units/bullets/items>", "shows all possible options for game content relevant to plugin config", (args) -> {
            String content = getContent(args[0]);
            Log.info((content != null ? content : "wrong option"));
        });

        handler.register("pew-load", "reloads configuration", (args) -> {
            shooter.load();
            Log.info("loaded");
        });

        handler.register("pew-yamlmode","<true/false>" , "set config file mode", (args) -> {
            String content = args[0];
            if(content != null) {
                if(content.equals("1") || content.toLowerCase().equals("true")) {
                    shooter.yamlmode(true);
                }
                else if(content.equals("0") || content.toLowerCase().equals("false")) {
                    shooter.yamlmode(false);
                }
            }
            Log.info("yamlmode=" + shooter.yamlmode());
        });

        if(debug) {
            handler.register("pew-mode", "show config file mode", (args) -> {
                Log.info("yamlmode=" + shooter.yamlmode());
            });
    
            handler.register("pew-test", "error log", (args) -> {
                Logging.log(new IOException("log file test"));
            });
        }
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("pew-info", "[unit] [item]", "shows info about implemented weapons", (args, player) -> {
            StringBuilder sb = new StringBuilder();
            HashMap<String, String> items = null;
            if(args.length == 0){
                sb.append(makeTitle("units with weapons"));
                for(String u : shooter.cfg.links.keySet()){
                    sb.append(u).append("\n");
                }
            }

            if(args.length >= 1) {
                items = shooter.cfg.links.get(args[0]);
                if(items == null) {
                    player.sendMessage("this unit is not supported by config");
                    return;
                }
            }

            if(args.length == 2) {
                String weapon = items.get(args[1]);
                if(weapon == null) {
                    player.sendMessage("this item is not supported by config");
                    return;
                }
                Stats stats = shooter.cfg.def.get(weapon);
                sb.append(makeTitle("weapon stats"));
                for(Field f : Shooter.Stats.class.getFields()) {
                    try{
                        sb.append(f.getName()).append(": ").append(f.get(stats)).append("\n");
                    }catch(IllegalAccessException e){
                        player.sendMessage("internal server error, sucks...");
                        return;
                    }
                }
            } else if (args.length == 1){
                sb.append(makeTitle("items in use"));
                for(String i : items.keySet()){
                    sb.append(i).append("\n");
                }
            }

            Call.infoMessage(player.con, sb.toString());
        });

        handler.<Player>register("pew-content", "<units/bullets/items>", "shows all possible units and items that can be related to weapon", (args, player) -> {
            String content = getContent(args[0]);
            if(content != null) {
                Call.infoMessage(player.con, makeTitle(args[0]) + content);
            } else {
                player.sendMessage("wrong option");
            }
        });
    }

    public String makeTitle(String text) {
        return "[orange]==" + text.toUpperCase() + "==[]\n\n";
    }

    public String getContent(String type) {
        switch(type){
            case "units":
                return listFields(UnitTypes.class);
            case "bullets":
                return listFields(Bullets.class);
            case "items":
                return listFields(Items.class);
            case "unit-weapons":
                String comment = "If unit offers 3 weapons, you can use 'spectre-1' to " +
                "'spectre-3' in place of bullet, mind that some weapons repeat as unit has one on each side.\n";
                try{
                    return comment + listUnitBullets();
                }catch(IllegalAccessException e){
                    return comment + "fatal error during reading content: " + e.getMessage();
                }
            default:
                return null;
        }
    }

    public static String listUnitBullets() throws IllegalAccessException{
        StringBuilder sb = new StringBuilder();
        for(Field f : UnitTypes.class.getFields()) {
            UnitType ut = (UnitType)f.get(null);
            sb.append(f.getName());
            for(Weapon w : ut.weapons) {
                sb.append(w.name).append(" ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public static <T> String listFields(Class<T> c) {
        StringBuilder sb = new StringBuilder();
        for (Field f : c.getFields()) {
            sb.append(f.getName()).append(" ");
        }
        return  sb.toString();
    }
}
