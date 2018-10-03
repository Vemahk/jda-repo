package me.vem.dbgm.permissions;

import java.util.HashMap;

import com.google.gson.Gson;

import me.vem.dbgm.cmd.SecureCommand;
import me.vem.jdab.utils.ExtFileManager;
import me.vem.jdab.utils.Logger;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

/**
 * 
 * @author Vemahk
 * PGData >> Permissions-Guild Data
 * This object is for storing all permissions information about a specific guild.
 */
public class PGData {
	
	private Guild guild;
	private HashMap<Long, Integer> rp; //rp >> Role Permissions
	private HashMap<Long, Integer> up; //up >> User Permissions
	
	private HashMap<SecureCommand, Integer> cp; //cp >> Command Permissions -- The required level of permissions required to use a certain command.
	
	private int dp = 0; //dp >> Default Permissions
	
	public PGData(Guild g) {
		this.guild = g;
		rp = new HashMap<>();
		up = new HashMap<>();
	}
	
	/**
	 * @param sc
	 * @param u
	 * @return true if the given user has the adequate permissions to execute the given command.
	 */
	public boolean canExecute(SecureCommand sc, User u) {
		return getPermission(u) >= cp.get(sc);
	}
	
	public PGData setDefaultPermissions(int x) {
		this.dp = x;
		return this;
	}
	
	public int getRolePermission(Role r) {
		if(rp.containsKey(r.getIdLong()))
			return rp.get(r.getIdLong());
		return dp;
	}
	
	public int getRolePermission(long id) {
		return getRolePermission(guild.getRoleById(id));
	}
	
	/**
	 * @param u >> The user
	 * @return -1 if the user is not apart of this guild.<br>Otherwise the user's highest permission given by rank or manually assigned permission level.
	 */
	public int getPermission(User u) {
		if(!guild.isMember(u)) //Shouldn't happen, but...
			return -1;
		
		if(up.containsKey(u.getIdLong())) //A user's presense in 'up' is meant to be overriding, thus it returns if present.
			return up.get(u.getIdLong());
		
		Member m = guild.getMember(u);
		
		int out = dp;
		for(Role r : m.getRoles())
			if(rp.containsKey(r.getIdLong())) {
				int rpi = rp.get(r.getIdLong());
				if(rpi > out)
					out = rpi;
			}
		
		return out;
	}

	/**
	 * @param r >> Role
	 * @param np >> New Permission level
	 */
	public void setRolePermission(Role r, int np) {
		if(r.getGuild() != guild) {//This shouldn't be possible... Just being sure, eh?
			Logger.err("Role permissions added to a guild it is not a part of?");
			return;
		}
		
		rp.put(r.getIdLong(), np);
	}
	
	/**
	 * setPermission should be the only method that adds a member to 'mp', since it is designed to be overriding of any role-given permissions.
	 * 
	 * @param u >> User
	 * @param np >> New Permission levels
	 */
	public void setPermission(User u, int np) {
		if(!guild.isMember(u)) { //Again, shouldn't be possible...
			Logger.err("Member permissions added to a guild it is not a part of?");
			return;
		}
		
		up.put(u.getIdLong(), np);
	}
	
	public String getJSON() {
		Gson gson = ExtFileManager.getGson();
		
		return null; //TODO finish
	}
	
	public static PGData fromJSON() {
		
		return null; //TODO finish
	}
}
