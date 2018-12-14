package me.vem.jdab.struct.menu;

import net.dv8tion.jda.core.entities.Message;

public class BuiltDefaultMenu extends DefaultMenu{

	private String[] pageList;
	
	public BuiltDefaultMenu(Message msg, String... pageList) {
		this(msg, 1, pageList);
	}
	
	public BuiltDefaultMenu(Message msg, int page, String... pageList) {
		super(msg, page);
		this.pageList = pageList;
	}

	@Override
	public void nextPage() {
		if(getPage() < pageList.length - 1)
			setPage(getPage() + 1);
	}

	@Override
	public String getResponse(int page) {
		return pageList[page-1];
	}
	
}
