package me.vem.jdab.struct.menu;

import net.dv8tion.jda.core.entities.Message;

public abstract class DefaultMenu extends Menu{

	public DefaultMenu(Message msg) { super(msg, 1); }
	public DefaultMenu(Message msg, int page) { super(msg, page); }
	
	@Override
	protected void update() {
		msg.editMessage(getResponse(getPage())).queue();
	}

	public abstract String getResponse(int page);
}
