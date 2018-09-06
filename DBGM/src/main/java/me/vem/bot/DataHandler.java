package me.vem.bot;

import java.io.IOException;

import me.vem.bot.cmd.Command;
import net.dv8tion.jda.core.JDA;

public interface DataHandler extends Command{
	public void saveData() throws IOException;
	public void loadData(JDA jda) throws IOException;
}
