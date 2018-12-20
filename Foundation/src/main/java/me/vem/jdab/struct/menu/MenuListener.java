package me.vem.jdab.struct.menu;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import me.vem.jdab.utils.Emoji;
import me.vem.jdab.utils.Task;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class MenuListener implements EventListener{

	private static MenuListener instance;
	public static MenuListener getInstance() {
		if(instance == null)
			instance = new MenuListener();
		return instance;
	}

	public static void unload() {
		if(instance == null)
			return;
		
		instance.timeout.cancel();
		Iterator<Menu> iter = instance.openMenues.iterator();
		while(iter.hasNext()) {
			iter.next().destroy();
			iter.remove();
		}
		
		instance = null;
	}
	
	private final List<Menu> openMenues;
	private final Timer timeout;
	
	private MenuListener() {
		timeout = new Timer("Menu Timer");
		openMenues = new LinkedList<>();
	}
	
	/**
	 * Schedules the destruction and removal of the given menu after '{@code delay}' seconds.
	 * @param menu
	 * @param delay
	 */
	public void timeout(Menu menu, int delay) {
		timeout.schedule(new Task(() -> {
			menu.destroy();
			MenuListener.this.openMenues.remove(menu);
		}), delay);
	}
	
	public void add(Menu m) {
		if(!openMenues.contains(m))
			openMenues.add(m);
	}
	
	public void remove(Menu m) {
		openMenues.remove(m);
	}
	
	@Override
	public void onEvent(Event event) {
		if(event instanceof MessageReactionAddEvent)
			addReaction((MessageReactionAddEvent)event);
		else if(event instanceof MessageReactionRemoveEvent)
			remReaction((MessageReactionRemoveEvent)event);
	}
	
	public static final Emoji LEFT_ARROW = new Emoji("\u2B05");
	public static final Emoji RIGHT_ARROW = new Emoji("\u27A1");
	public static final Emoji CANCEL = new Emoji("\u274C");
	
	private void addReaction(MessageReactionAddEvent event) {
		if(event.getUser().equals(event.getJDA().getSelfUser()))
			return;
		
		Emoji reaction = new Emoji(event.getReactionEmote());
		
		if(reaction.equals(LEFT_ARROW) || reaction.equals(RIGHT_ARROW)){
			for(Menu menu : openMenues)
				if(menu.matches(event.getMessageIdLong()))
					if(reaction.equals(LEFT_ARROW))
						menu.prevPage();
					else menu.nextPage();
		}else if(reaction.equals(CANCEL)){
			Iterator<Menu> iter = openMenues.iterator();
			while(iter.hasNext()) {
				Menu next = iter.next();
				if(next.matches(event.getMessageIdLong())) {
					next.destroy();
					iter.remove();
					break;
				}
			}
		}
	}
	
	private void remReaction(MessageReactionRemoveEvent event) {
		if(event.getUser().equals(event.getJDA().getSelfUser()))
			return;
		
		Emoji reaction = new Emoji(event.getReactionEmote());
		
		//Look, ma! No brackets!
		if(reaction.equals(LEFT_ARROW) || reaction.equals(RIGHT_ARROW))
			for(Menu menu : openMenues)
				if(menu.matches(event.getMessageIdLong()))
					if(reaction.equals(LEFT_ARROW))
						menu.prevPage();
					else menu.nextPage();
	}
}