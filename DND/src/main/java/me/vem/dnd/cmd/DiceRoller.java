package me.vem.dnd.cmd;

import java.awt.Color;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.vem.jdab.DiscordBot;
import me.vem.jdab.cmd.Command;
import me.vem.jdab.cmd.Configurable;
import me.vem.jdab.struct.Pair;
import me.vem.jdab.struct.SelfPurgeList;
import me.vem.jdab.struct.menu.EmbedMenu;
import me.vem.jdab.struct.menu.Menu;
import me.vem.jdab.struct.menu.MenuListener;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import me.vem.jdab.utils.Respond;
import me.vem.jdab.utils.Utilities;
import me.vem.jdab.utils.confirm.Request;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class DiceRoller extends Command implements Configurable, EventListener{

	private static final Pattern dicePattern = Pattern.compile("d(\\d+)((\\+|-)(\\d+))?");
	
	private static DiceRoller instance;
	public static DiceRoller getInstance() {
		if(instance == null)
			instance = new DiceRoller();
		return instance;
	}
	
	/**
	 * Long: guild id
	 */
	private Map<Long, DiceInfo> database;
	
	private DiceRoller() {
		super("roll");
		load();
	}
	
	@Override
	public boolean run(GuildMessageReceivedEvent event, String... args) {
		if(!super.run(event, args))
			return false;
		
		if(database.containsKey(event.getGuild().getIdLong())){
			DiceInfo info = database.get(event.getGuild().getIdLong());
			if(info.getChannelId() != 0) { //If the channel is inited.
				if(event.getChannel().getIdLong() != info.getChannelId())
					Respond.async(event.getChannel(), "I cannot run this command. There is already a dice channel active.");
				
				return false;
			}
		}
		
		if(args.length == 0)
			return sendHelp(event.getChannel(), true);
		
		if("init".equals(args[0])) {
			new Request(event.getAuthor(), event.getChannel(), "Setting this channel as the dice channel will purge it of all messages, and the bot will delete all future messages until it is uninited.",
				()->{ //Success
					DiceInfo localInfo = new DiceInfo();
					database.put(event.getGuild().getIdLong(), localInfo);
					localInfo.setup(event.getChannel());
				}, ()->{});
		}
		
		return true;
	}
	
	@Override
	public String getDescription() {
		return "Allows the Game Masters to have a dedicated room for rolling dice and keeping a history of all dice rolls.";
	}

	@Override
	public String[] usages() {
		return new String[] {
			"`roll init` -- Sets up the current channel to become the dice channel.",
			"`uninit` -- If the current channel is the current dice channel, it removes that restriction on it.",
			"Note: for `uninit`, because the bot is scanning the channel for *all* messages, don't type the bot's prefix and the command roll. Just type `uninit`"
		};
	}

	@Override
	public boolean hasPermissions(GuildMessageReceivedEvent event, String... args) {
		return event.getMember().hasPermission(Permission.ADMINISTRATOR);
	}

	@Override
	public void save() {
		if(Utilities.saveToJSONFile("dice.json", database))
			Logger.info("Dice database saved.");
		else Logger.err("A problem occured during the DiceRoller save.");
	}

	@Override
	public void load() {
		database = new LinkedHashMap<>();
		
		File configFile = ExtFileManager.getConfigFile("dice.json");
		if(configFile == null) return;
		
		String content = ExtFileManager.readFileAsString(configFile);
		if(content == null || content.length() == 0) return;
		
		Gson gson = ExtFileManager.getGsonPretty();
		database = gson.fromJson(content, new TypeToken<LinkedHashMap<Long, DiceInfo>>(){}.getType());
		
		//I think this is the first load that has gone past this point...
		for(long l : database.keySet()) {
			Guild guild = DiscordBot.getInstance().getJDA().getGuildById(l);
			DiceInfo info = database.get(l);
			info.setup(guild.getTextChannelById(info.getChannelId()));
		}
	}

	@Override
	protected void unload() {
		save();
		
		for(DiceInfo info : database.values()) {
			if(info.shutdown())
				Respond.sync(info.textChannel, new EmbedBuilder().setColor(Color.RED).setTitle("WARNING").setDescription("This channel is set as the bot's Dice Channel. When the bot is restarted, this channel will be purged of all messages.").build());
		}
		
		DiscordBot.getInstance().getJDA().removeEventListener(this);
		
		instance = null;
	}
	
	@Override
	public void onEvent(Event event) {
		if(event instanceof GuildMessageReceivedEvent)
			messageReceived((GuildMessageReceivedEvent) event);
	}
	
	private void messageReceived(GuildMessageReceivedEvent event) {
		if(event.getAuthor().equals(event.getJDA().getSelfUser()))
			return;
		
		long guild = event.getGuild().getIdLong();
		if(database.containsKey(guild)) {
			DiceInfo info = database.get(guild);
			if(info.getChannelId() != event.getChannel().getIdLong())
				return;
			
			String text = event.getMessage().getContentRaw();
			if("uninit".equals(text)) {
				info.shutdown();
				database.remove(guild);
			}else {
				info.roll(event.getMember(), text);
			}
			
			event.getMessage().delete().queue();
		}
	}
	
	private class DiceInfo{
		
		private transient TextChannel textChannel;
		private transient Message instr;
		private transient Menu historyMenu;
		
		private long channel;
		private LinkedList<Pair<String, String>> history;
		
		public DiceInfo() {
			history = new LinkedList<>();
		}
		
		public long getChannelId() {
			return channel;
		}
		
		public void roll(Member member, String roll) {
			String parsed = parseRoll(roll);
			if(parsed == null)
				return;
			
			history.addFirst(new Pair<>(parsed, "Rolled by: " + member.getEffectiveName()));
			historyMenu.setPage(historyMenu.getPage());
		}
		
		/**
		 * Foolproof. (This is a lie, of course)
		 * @param roll The supposed roll.
		 * @return null if the roll format is invalid. A full descriptive string of the roll otherwise. E.g. for d20+3: [16], [13+3]
		 */
		private String parseRoll(String roll) {
			Matcher matcher = dicePattern.matcher(roll);
			if(!matcher.matches())
				return null;
			
			boolean modified = matcher.group(2) != null;
			
			int faces = Integer.parseInt(matcher.group(1));
			int modifier = modified ? (matcher.group(3).equals("+") ? 1 : -1) * Integer.parseInt(matcher.group(4)) : 0;
			
			int real = (int)(Math.random() * faces) + 1;
			int effective = real + modifier;
			if(effective > faces) effective = faces;
			else if(effective < 1) effective = 1;
			
			StringBuilder parsed = new StringBuilder().append('[').append(effective).append(']');
			if(modifier != 0)
				parsed.append(", [").append(real).append(String.format("%+d", modifier)).append(']');
			
			return parsed.toString();
		}
		
		private EmbedBuilder getHistoryPage(int page) {
			EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED).setTitle("Dice History - Page " + page);
			
			if(page < 1)
				return builder.addField("No such page", "", false);
			
			Iterator<Pair<String, String>> iter = history.iterator();
			for(int i=0; i < (page - 1) * 5;i++, iter.next())
				if(!iter.hasNext())
					return builder.addField("No such page", "", false);
			
			for(int x=0;x<5;x++) {
				if(!iter.hasNext()) {
					if(x == 0)
						return builder.addField("No such page", "", false);
					break;
				}
				Pair<String, String> next = iter.next();
				builder.addField(next.getFirst(), next.getSecond(), false);
			}
			
			return builder.setColor(Color.CYAN);
		}
		
		public void setup(TextChannel channel) {
			this.textChannel = channel;
			this.channel = channel.getIdLong();
			
			SelfPurgeList purge = new SelfPurgeList(channel);
			for(Message msg : channel.getIterableHistory().cache(false))
				purge.add(msg);
			purge.clear();
			
			//Wow. This is trashy.
			MessageEmbed instructions = new EmbedBuilder()
					.setColor(Color.ORANGE)
					.setTitle("Dice Roller Instructions")
					.setDescription("This bot is monitoring this channel for dice rolls. Don't type a command, just type a dice roll. Everything else will be ignored and deleted.")
					.addField("Examples:", 
							"`d20` -- Rolls a d20 (e.g. `[16]`)\n"
						  + "`d20+5` -- Rolls a d20 and adds 5 (e.g. `[12+5]`.\n"
						  + "`d6-1` -- Rolls a d6 and subtracts 1."
							, false)
					.addField("Note:",
							"The effective value (min: 1, max: #faces) and the real value (rolled +/- modifier) will both be displayed.\n"
						  + "The name of the person who rolled them will be in the description of the fields in the history."
							, false)
					.build();
			
			instr = Respond.sync(channel, instructions);
			
			historyMenu = new EmbedMenu(Respond.sync(channel, getHistoryPage(1))) {
				@Override
				public MessageEmbed getEmbed(int page) {
					return getHistoryPage(page).build();
				}
			};
		}
		
		public boolean shutdown() {
			if(channel == 0) return false;
			historyMenu.destroy();
			MenuListener.getInstance().remove(historyMenu);
			instr.delete().queue();
			return true;
		}
	}
}
