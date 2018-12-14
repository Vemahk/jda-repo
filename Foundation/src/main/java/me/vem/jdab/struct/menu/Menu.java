package me.vem.jdab.struct.menu;

import net.dv8tion.jda.core.entities.Message;

public abstract class Menu {
	protected final Message msg;
	private int page;

	public Menu(Message msg) { this(msg, 1); }

	public Menu(Message msg, int page) {
		this.page = page;
		this.msg = msg;
		
		msg.addReaction(MenuListener.LEFT_ARROW.toString()).queue();
		msg.addReaction(MenuListener.RIGHT_ARROW.toString()).queue();
		msg.addReaction(MenuListener.CANCEL.toString()).queue();

		MenuListener.getInstance().add(this);
	}
	
	/**
	 * Updates {@code msg}, for if the page number is changed.
	 */
	protected abstract void update();

	private boolean isScheduled = false;
	/**
	 * @param delay The number of seconds until this menu is to be destroyed.
	 * @return true if this menu object is not already scheduled to be removed. False otherwise.
	 */
	public boolean setTimeout(int delay) {
		if(isScheduled)
			return false;
		
		MenuListener.getInstance().timeout(this, delay);
		
		isScheduled = true;
		return true;
	}
	
	public int getPage() { return page; }
	
	/**
	 * @param l A snowflake ID.
	 * @return true if the passed long matches the snowflake id of the message of this menu.
	 */
	public boolean matches(long l) {
		return l == msg.getIdLong();
	}
	
	public void setPage(int page) {
		this.page = page;
		update();
	}
	
	public void nextPage() {
		page++;
		update();
	}
	
	public void prevPage() {
		if(page == 1)
			return;
		page--;
		update();
	}
	
	public void destroy() {
		msg.delete().queue((success) -> {}, (failure) -> {});
	}
}
