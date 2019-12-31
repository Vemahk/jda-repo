package me.vem.dbgm.cmd;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class Stopwatch extends Command implements Configurable {

    private static Stopwatch instance;
    public static Stopwatch getInstance() { return instance; }
    public static void initialize() {
        if(instance == null)
            instance = new Stopwatch();
    }
    
    private Map<Long, LinkedHashMap<String, Long>> database;
    
    protected Stopwatch() {
        super("stopwatch");
        load();
    }

    @Override public boolean run(GuildMessageReceivedEvent event, String... args) {
        if(!super.run(event, args))
            return false;
        
        if(args.length == 0)
            return sendHelp(event.getChannel(), false);
        
        LinkedHashMap<String, Long> guildDatabase = database.get(event.getGuild().getIdLong());
        if(guildDatabase == null) {
            guildDatabase = new LinkedHashMap<>();
            database.put(event.getGuild().getIdLong(), guildDatabase);
        }
        
        if("start".equals(args[0])) {
            if(args.length < 2)
                return sendHelp(event.getChannel(), false);
            
            if(guildDatabase.put(args[1], System.currentTimeMillis()) == null) {
                Respond.asyncf(event.getChannel(), "Stopwatch `%s` has been started!", args[1]);
            }else {
                Respond.asyncf(event.getChannel(), "`%s` is already a running stopwatch.", args[1]);
                return false;
            }
            
        }else if("stop".equals(args[0])) {
            if(args.length < 2)
                return sendHelp(event.getChannel(), false);
            
            Long time = guildDatabase.remove(args[1]);
            if(time != null) {
                Respond.asyncf(event.getChannel(), "Stopped. `%s` was active for " + Utilities.formatTime(System.currentTimeMillis() - time) + ".", args[1]);
            }else {
                Respond.asyncf(event.getChannel(), "`%s` is not an active stopwatch.", args[1]);
                return false;
            }
            
        }else if(guildDatabase.containsKey(args[0])){
            long timeSinceStopwatchBegan = guildDatabase.get(args[0]);
            Respond.asyncf(event.getChannel(), "`%s` has been active for " + Utilities.formatTime(System.currentTimeMillis() - timeSinceStopwatchBegan) + ".", args[0]);
            
        }else {
            Respond.asyncf(event.getChannel(), "`%s` is not an active stopwatch.", args[0]);
            return false;
            
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Allows users to create, check, and remove stopwatches by a certain name. For when we want to know how long it's been since something.";
    }

    @Override
    public String[] usages() {
        return new String[] {
            "`stopwatch <stopwatch_name>` Gets the time since a certain stopwatch was begun.",
            "`stopwatch start <stopwatch_name>` Begins a new stopwatch with a particular name.",
            "`stopwatch stop <stopwatch_name>` Stops and deletes a stopwatch by its name. Will let you know its runtime, too."
        };
    }

    @Override
    public boolean hasPermissions(Member member, String... args) {
        return true;
    }

    @Override
    protected void unload() {
        instance = null;
        save();
    }

    @Override
    public void save() {
        ExtFileManager.saveObjectAsJson("stopwatch.json", database);
        Logger.infof("Stopwatch database saved...");
    }

    @Override
    public void load() {
        database = new LinkedHashMap<>();
        
        File configFile = ExtFileManager.getConfigFile("stopwatch.json");
        if(configFile == null) return;
        
        String content = ExtFileManager.readFileAsString(configFile);
        if(content == null || content.length() == 0) return;
        
        Gson gson = ExtFileManager.getGsonPretty();
        database = gson.fromJson(content, new TypeToken<LinkedHashMap<Long, LinkedHashMap<String, Long>>>(){}.getType());
    }
}