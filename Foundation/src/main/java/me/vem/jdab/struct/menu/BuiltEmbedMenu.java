package me.vem.jdab.struct.menu;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class BuiltEmbedMenu extends EmbedMenu{

	private MessageEmbed[] pageList;
	
	public BuiltEmbedMenu(Message msg, MessageEmbed... pageList) {
		this(msg, 1, pageList);
	}

	public BuiltEmbedMenu(Message msg, int page, MessageEmbed... pageList) {
		super(msg, page);
		this.pageList = pageList;
	}
	
	@Override
	public void nextPage() {
		if(getPage() < pageList.length - 1)
			setPage(getPage() + 1);
	}
	
	@Override
	public MessageEmbed getEmbed(int page) {
		return pageList[page-1];
	}
}
