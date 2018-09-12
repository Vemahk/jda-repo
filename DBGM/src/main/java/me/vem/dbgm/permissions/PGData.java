package me.vem.dbgm.permissions;

import java.util.HashMap;

import me.vem.dbgm.cmd.SecureCommand;
import me.vem.dbgm.utils.Logger;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

/**
 * 
 * @author Vemahk
 * PGData >> Permissions-Guild Data
 * This object is for storing all permissions information about a specific guild.
 */
public class PGData {
	
	private Guild guild;
	private HashMap<Role, Integer> rp; //rp >> Role Permissions
	private HashMap<Member, Integer> mp; //mp >> Member Permissions
	
	private HashMap<SecureCommand, Integer> cp; //cp >> Command Permissions -- The required level of permissions required to use a certain command.
	
	private int dp = 0; //dp >> Default Permissions
	
	public PGData(Guild g) {
		this.guild = g;
		rp = new HashMap<>();
		mp = new HashMap<>();
	}
	
	public PGData setDefaultPermissions(int x) {
		this.dp = x;
		return this;
	}
	
	public int getRolePermission(Role r) {
		if(rp.containsKey(r))
			return rp.get(r);
		return dp;
	}
	
	public int getRolePermission(long id) {
		return getRolePermission(guild.getRoleById(id));
	}
	
	public int getPermission(Member m) {
		if(mp.containsKey(m)) //A member's presense in 'mp' is meant to be overriding, thus it returns if present.
			return mp.get(m);

		int out = dp;
		for(Role r : m.getRoles())
			if(rp.containsKey(r)) {
				int rpi = rp.get(r);
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
		
		rp.put(r, np);
	}
	
	/**
	 * setPermission should be the only method that adds a member to 'mp', since it is designed to be overriding of any role-given permissions.
	 * 
	 * @param m >> Member
	 * @param np >> New Permission levels
	 */
	public void setPermission(Member m, int np) {
		if(m.getGuild() != guild) { //Again, shouldn't be possible...
			Logger.err("Member permissions added to a guild it is not a part of?");
			return;
		}
		
		mp.put(m, np);
	}
	
	private String roleOutFormat() {
		StringBuffer out = new StringBuffer("{");
		
		//TODO
		
		out.append("}");
		return out.toString();
	}
	
	private String memOutFormat() {
		StringBuffer out = new StringBuffer("{");
		
		//TODO
		
		out.append("}");
		return out.toString();
	}
}
