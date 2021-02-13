package pew;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import org.junit.platform.commons.util.*;

import java.io.*;
import java.text.*;
import java.util.*;

public class Shooter {
    public static final String dir = "config/mods/pewpew/";
    public static final String errors = dir + "errors/";
    public static final String config = dir + "config.json";

    public HashMap<UnitType, HashMap<Item, Weapon>> weapons = new HashMap<>();
    public Config cfg = new Config();

    public Seq<Data>
        units = new Seq<>(),
        players = new Seq<>(),
        garbage = new Seq<>();

    public Shooter() {
        Logging.on(EventType.UnitCreateEvent.class, e -> units.add(garbage.pop(() -> new Data(e.unit))));

        Logging.on(EventType.PlayerJoin.class, e -> players.add(garbage.pop(() -> new Data(e.player))));

        Boolf<Data> update = d -> {
            Unit u = d.unit;

            // reload cycle
            d.reloadProgress += Time.delta/60;

            // removing garbage except player spawns
            if(u.dead && !u.spawnedByCore()) {
                garbage.add(d);
                return false;
            }

            if(!u.isShooting) {
                return true;
            }

            HashMap<Item, Weapon> inner = weapons.get(u.type);
            if(inner == null) {
                return true;
            }

            Weapon w = inner.get(u.item());
            if(w == null) {
                return true;
            }

            w.shoot(d);
            return true;
        };

        Logging.run(Trigger.update, () -> {
            units.filter(update);

            players.filter(d -> {
                d.unit = d.player.unit();

                if(d.unit == null) {
                    return true;
                }

                if(!d.player.con.isConnected()) {
                    garbage.add(d);
                    return false;
                }

                if(d.unit.spawnedByCore()) {
                    return update.get(d);
                }

                return true;
            });
        });

        load();
    }

    public void load(){
        try{
            Config cfg = Util.load(config, Config.class);
            if(cfg == null) {
                return;
            }

            this.cfg = cfg;
            weapons = cfg.parse();
        } catch(IOException e) {
            Log.info("failed to parse config file: " + e.getMessage());
        } catch(Exception e) {
            this.cfg.links = new HashMap<>();
            this.cfg.def = new HashMap<>();
            Log.info("failed to load weapons: " + e.getMessage());
        }
    }

    // config represents game configuration
    public static class Config {
        public HashMap<String, Stats> def = new HashMap<String, Stats>(){{
            put("copperGun", new Stats());
        }};

        public HashMap<String, HashMap<String, String>> links = new HashMap<String, HashMap<String, String>>(){{
            put("alpha", new HashMap<String, String>(){{
                put("copper", "copperGun");
            }});
            put("beta", new HashMap<String, String>(){{
                put("copper", "copperGun");
            }});
            put("gamma", new HashMap<String, String>(){{
                put("copper", "copperGun");
            }});
        }};

        @JsonIgnore
        public HashMap<UnitType, HashMap<Item, Weapon>> parse() throws Exception {
            HashMap<UnitType, HashMap<Item, Weapon>> res = new HashMap<>();
            for(String unit : links.keySet()) {
                UnitType ut = (UnitType)Util.getProp(UnitTypes.class, unit);

                HashMap<Item, Weapon> inner = res.computeIfAbsent(ut, k -> new HashMap<>());
                for(String item : links.get(unit).keySet()) {
                    Item i = (Item)Util.getProp(Items.class, item);
                    String defName = links.get(unit).get(item);
                    Stats stats = def.get(defName);
                    if(stats == null) {
                        throw new Exception(String.format("%s does not exist in weapon definitions", defName));
                    }

                    inner.put(i, new Weapon(stats, ut, defName));
                }
            }
            return res;
        }

    }

    public static class Data {
        Unit unit;
        Player player;
        float reloadProgress;
        int ammo;

        public Data(Unit u) {
            this.unit = u;
        }

        public Data(Player p) {
            this.player = p;
        }
    }

    public static class Stats {
        public String
            bullet = "standardCopper"; //bullet type
        public float
            inaccuracy = 2f, // in degrees
            damageMultiplier = 1f,
            reload = .3f; // in seconds
        public int
            bulletsPerShot = 2,
            ammoMultiplier = 4,
            itemsPerScoop = 1;
    }

    public static class Weapon {
        public BulletType bullet, original;
        public Stats stats;

        // helper vectors to reduce allocations.
        static Vec2 h1 = new Vec2(), h2 = new Vec2(), h3 = new Vec2();

        public Weapon(Stats stats, UnitType ut, String name) throws Exception {
            this.stats = stats;

            try{

                if(stats.bullet.contains("-")) {
                    this.bullet = Util.getUnitBullet(stats.bullet, ut);
                } else {
                    this.bullet = (BulletType)Util.getProp(Bullets.class, stats.bullet);
                }
            } catch(Exception e) {
                throw new Exception("weapon with name '"+name+"' is invalid: " + e.getMessage());
            }



            // finding bullet with biggest range
            ut.weapons.forEach(w -> {
                if(original == null || original.range() < w.bullet.range()) {
                    original = w.bullet;
                }
            });
        }

        public void shoot(Data d){
            Unit u = d.unit;
            if(d.reloadProgress < stats.reload) {
                return;
            }
            d.reloadProgress = 0;

            // refilling ammo
            if(d.ammo == 0) {
                // not enough items to get new ammo
                if(u.stack.amount < stats.itemsPerScoop) {
                    return;
                }

                u.stack.amount -= stats.itemsPerScoop;
                d.ammo += stats.ammoMultiplier;
            }

            d.ammo--;

            h1
            .set(original.range(), 0) // set length to range
            .rotate(h2.set(u.aimX, u.aimY).sub(u.x, u.y).angle()) // rotate to shooting direction
            .add(h3.set(u.vel).scl(60f * Time.delta)); // add velocity offset

            // its math
            float vel = h1.len() / original.lifetime / bullet.speed;
            float life = original.lifetime / bullet.lifetime;
            float dir = h1.angle();

            if(!bullet.collides) {
                // h2 is already in state of vector from u.pos to u.aim and we only care about length
                life = h2.len() / bullet.range(); // bullet is controlled by cursor
            }

            for(int i = 0; i < stats.bulletsPerShot; i++){
                Call.createBullet(
                    bullet,
                    u.team,
                    u.x, u.y,
                    dir + Mathf.range(-stats.inaccuracy, stats.inaccuracy), // apply inaccuracy
                    stats.damageMultiplier * bullet.damage,
                    vel,
                    life
                );
            }
        }
    }

    public static class Util {
        public static <T> Object getProp(Class<T> target, String prop) throws Exception {
            try{
                return target.getField(prop).get(null);
            }catch(NoSuchFieldException ex){
                throw new Exception(String.format(
                    "%s does not contain '%s', use 'content %s' to see the options",
                    target.getSimpleName(),
                    prop,
                    target.getSimpleName().toLowerCase().replace("type", "")
                ));
            }catch(IllegalAccessException ex){
                throw new RuntimeException(ex);
            }
        }

        public static BulletType getUnitBullet(String ptr, UnitType ut) throws Exception {
            String[] parts = ptr.split("-");
            if(parts.length != 2) {
                throw new Exception("the unit bullet has to be unit name, and weapon index separated by '-'");
            }
            if (!parts[0].equals("self")) {
                ut = (UnitType)getProp(UnitTypes.class, parts[0]);
            }
            if(!Strings.canParsePositiveInt(parts[1])) {
                throw new Exception("cannot parse " + parts[1] + " to integer");
            }

            int idx = Integer.parseInt(parts[1]) - 1;
            if(idx >= ut.weapons.size || idx < 0) {
                throw new Exception("the maximal weapon is " + ut.weapons.size + " and min is 1, you entered " + parts[1]);
            }

            return  ut.weapons.get(idx).bullet;
        }

        public static <T> T load(String filename, Class<T> type) throws IOException{
            ObjectMapper mapper = new ObjectMapper();
            File f = new File(filename);
            if(!f.exists()){
                return save(filename, type);
            }
            return mapper.readValue(f, type);
        }

        public static <T> T save(String filename, Class<T> type) throws IOException{
            ObjectMapper mapper = new ObjectMapper();
            makeFullPath(filename);
            File f = new File(filename);
            T obj;
            try{
                obj = type.getDeclaredConstructor().newInstance();
            }catch(Exception e){
                e.printStackTrace();
                Log.info("Please copy this error and open the issue with it on github.");
                return null;
            }
            mapper.writeValue(f, obj);
            return obj;
        }

        public static void makeFullPath(String filename) throws IOException{
            File targetFile = new File(filename);
            File parent = targetFile.getParentFile();
            if(!parent.exists() && !parent.mkdirs()){
                throw new IOException("Couldn't create dir: " + parent);
            }
        }
    }



    public static class Logging {
        public static void log(Throwable t) {
            String ex = ExceptionUtils.readStackTrace(t);
            SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH-mm-ss-SSS z");
            java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
            File f = new File(errors+formatter.format(date));
            t.printStackTrace();
            try {
                Util.makeFullPath(f.getAbsolutePath());
                boolean create = f.createNewFile();
                if(!create) {
                    throw new IOException("log file on current time already existas which should not be possible");
                }
                PrintWriter out = new PrintWriter(f.getAbsolutePath());

                out.println(ex);
                out.close();
            } catch(IOException e) { e.printStackTrace();}

        }

        public static <T> void on(Class<T> event, Cons<T> cons) {
            Events.on(event, e -> {
                try{
                    cons.get(e);
                } catch(Exception ex) {
                    log(ex);
                }
            });
        }

        public static void run(Object event, Runnable listener) {
            Events.run(event, () -> {
                try{
                    listener.run();
                } catch(Exception ex) {
                    log(ex);
                }
            });
        }
    }
}
