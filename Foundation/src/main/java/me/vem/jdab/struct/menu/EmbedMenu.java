package me.vem.jdab.struct.menu;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;

public abstract class EmbedMenu extends Menu{

	public EmbedMenu(Message msg) { super(msg, 1); }
	public EmbedMenu(Message msg, int page) { super(msg, page); }
	
	@Override
	protected void update() {
		msg.editMessage(getEmbed(getPage())).queue();
	}
	
	public abstract MessageEmbed getEmbed(int page);
}
