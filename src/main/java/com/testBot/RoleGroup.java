package com.testBot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import net.dv8tion.jda.core.entities.*;

public class RoleGroup {

    private Connection conn;
    private List<RoleData> roles;
    private String type;
    private Long boundRole;
    private BotGuild guild;
    private Long groupId;
    private String groupName;

    public List<RoleData> getRoles() {
        return roles;
    }

    public String getType() {
        return type;
    }

    public Long getBoundRole() {
        return boundRole;
    }

    public BotGuild getGuild() {
        return guild;
    }

    public String getGroupName() {
        return groupName;
    }

    public String command(Guild guild, Member member, String rolename)
    {
        RoleData rd;
        StringBuilder ret = new StringBuilder();
        switch (type)
        {
            case "LIST":
                rd = RoleData.find(roles,rolename);
                if(rd==null)
                {
                    System.out.print("grouproles custom - wrong syntax");
                    ret.append("wrong syntax\nlook at help");
                }else {
                    Role role = guild.getRoleById(rd.getRoleId());
                    if (guild.getSelfMember().getRoles().get(0).getPosition() > role.getPosition()) {
                        if (memberHasRole(member, rd.getRoleId())) {
                            guild.getController().removeRolesFromMember(member, role).queue();
                            ret.append("Role ").append(role.getName()).append(" removed");
                            System.out.print("grouproles custom - role removed");
                        } else {
                            guild.getController().addRolesToMember(member, role).queue();
                            ret.append("Role ").append(role.getName()).append(" added");
                            System.out.print("grouproles custom - role added");
                        }
                    }else{
                        ret.append("Role ").append(role.getName()).append(" higher than my highest role");
                        System.out.print("grouproles custom - too low role");
                    }
                }
                break;
        }
        return ret.toString();
    }



    public String modify(String[] args,Message message)
    {
        ResourceBundle outputs = guild.getMessages();
        Statement stmt;
        StringBuilder retStr = new StringBuilder();
        switch (args[0])
        {
            case "add":
                //get mention list
                List<Role> list = message.getMentionedRoles();
                //if there is a mention and the syintax is correct
                if(list.size()==1 && args.length == 4 && args[2].equals("as"))
                {
                    //if the name is not too long
                    if( args[3].length()<=10) {
                        if (RoleData.find(roles, list.get(0).getIdLong()) == null) {
                            if (RoleData.find(roles, args[3]) == null) {
                                try {
                                    stmt = conn.createStatement();
                                    Long id = list.get(0).getIdLong();
                                    stmt.execute("INSERT INTO grouproles(groupid, roleid, rolename) VALUES (" + this.groupId + "," + id + ",'" + args[3] + "')");
                                    stmt.execute("COMMIT");
                                    roles.add(new RoleData(args[3], list.get(0).getIdLong()));
                                    stmt.close();
                                    retStr.append("Role correctly added");
                                    System.out.print("grouproles - role added ");
                                } catch (SQLException ex) {
                                    System.out.println("SQLException: " + ex.getMessage());
                                    System.out.println("SQLState: " + ex.getSQLState());
                                    System.out.println("VendorError: " + ex.getErrorCode());
                                    System.exit(-1);
                                    retStr.append("error adding role");
                                    System.out.print("grouproles - error on role ");
                                }
                            }else
                            {
                                System.out.print("grouproles - found existing nick ");
                                retStr.append("that nick is already used");
                            }
                        }else{
                            System.out.print("grouproles - found existing role ");
                            retStr.append("that role is already included");
                        }
                    }else {
                        System.out.print("grouproles - name limit exceed ");
                        retStr.append("error name too long limit to 10 char");
                    }
                }else{
                    System.out.print("grouproles - wrong syntax");
                    retStr.append("wrong syntax");
                }
                break;

            case "remove":
                if(args[1]!=null)
                {
                    RoleData role = RoleData.find(roles,args[1]);
                    if(role!=null)
                    {
                        try {
                            stmt = conn.createStatement();
                            stmt.execute("DELETE FROM grouproles WHERE groupid="+groupId+" AND roleid="+role.getRoleId());
                            stmt.execute("COMMIT");
                            roles.remove(role);
                            stmt.close();
                            System.out.println("grouproles - role removed");
                            retStr.append("Role correctly removed");
                        } catch (SQLException ex) {
                            System.out.println("SQLException: " + ex.getMessage());
                            System.out.println("SQLState: " + ex.getSQLState());
                            System.out.println("VendorError: " + ex.getErrorCode());
                            System.exit(-1);
                            System.out.print("grouproles - error on role");
                            retStr.append("error on role");
                        }
                    }else{
                        System.out.print("grouproles - role not found");
                        retStr.append("wrong syntax");
                    }
                }else{
                    System.out.print("grouproles - wrong syntax");
                    retStr.append("wrong syntax");
                }
                break;

            case "type":
                if(args[1]!=null)
                {
                    switch (args[1])
                    {
                        case "list":
                        case "List":
                        case "LIST":
                            try {
                                stmt = conn.createStatement();
                                stmt.execute("UPDATE groups SET type WHERE groupid="+groupId+" VALUE '"+args[1].toUpperCase()+"'");
                                stmt.execute("COMMIT");
                                this.type = args[1].toUpperCase();
                                stmt.close();
                                System.out.print("grouproles - type udated");
                                retStr.append("type updated");
                            } catch (SQLException ex) {
                                System.out.println("SQLException: " + ex.getMessage());
                                System.out.println("SQLState: " + ex.getSQLState());
                                System.out.println("VendorError: " + ex.getErrorCode());
                                System.exit(-1);
                                System.out.print("grouproles - error in type");
                                retStr.append("error on type");
                            }
                            break;
                        default :
                            System.out.print("grouproles - type not found");
                            retStr.append("type not found");

                    }
                }else
                {
                    System.out.print("grouproles - wrong syntax");
                    retStr.append("wrong syntax");
                }
                break;
            case "roles":
            {
                Guild guild = message.getGuild();
                retStr.append(outputs.getString("rolegroup-role-listing")).append(" ").append(groupName).append("\n");
                for (RoleData role : roles)
                {
                    retStr.append(guild.getRoleById(role.getRoleId()).getName()).append("\tas ");
                    retStr.append(role.getRoleName()).append("\n");
                }
                System.out.print("grouproles - listing roles of "+ groupName);
            }
            break;
            default:
                System.out.print("grouproles - wrong syntax");
                retStr.append("wrong syntax");
                break;

        }
        return retStr.toString();
    }

    public boolean isValid()
    {
        return roles.size() > 0;
    }

    public String printHelp()
    {
        StringBuilder ret = new StringBuilder(groupName).append(" ");
        switch (type)
        {
            case "LIST":
                for(RoleData role : roles)
                {
                    ret.append(role.getRoleName()).append("/");
                }
                if(roles.size()>0)
                    ret.deleteCharAt(ret.lastIndexOf("/"));
        }
        return ret.toString();
    }




    public RoleGroup(Connection conn, BotGuild guild, Long groupId, String groupName) {
        this.conn = conn;
        this.guild = guild;
        this.groupId=groupId;
        this.groupName = groupName;
        this.roles = new ArrayList<>();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT type,roleid FROM groups WHERE groupid=" + groupId);

                if (rs.next()) {
                    this.type = rs.getString(1);
                    this.boundRole = rs.getLong(2);
                    rs.close();
                    rs = stmt.executeQuery("SELECT roleid,rolename FROM grouproles WHERE groupid=" + groupId);
                    this.roles.clear();
                    while (rs.next()) {
                        this.roles.add(new RoleData(rs.getString(2),rs.getLong(1)));
                    }
                    rs.close();
                } else {
                    this.roles.clear();
                    System.out.println("error id not found");
                }
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    public RoleGroup(Connection conn, BotGuild guild,Role role, String groupName) {
        this.conn = conn;
        this.guild = guild;
        this.groupName = groupName;
        this.roles = new ArrayList<>();
        Long guildId = guild.getId();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            stmt.execute("INSERT INTO groups (guildid,groupname,type,roleid) VALUES ("+guildId+",'"+groupName+"','LIST',"+role.getIdLong()+")");
            rs = stmt.executeQuery("SELECT groupid FROM groups WHERE guildid="+guildId+" AND groupname='"+groupName+"'");
            if(rs.next())
                this.groupId=rs.getLong(1);
            stmt.execute("COMMIT");
            this.boundRole = role.getIdLong();
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    public void delete()
    {
        Statement stmt;
        try {
            stmt = conn.createStatement();
            stmt.execute("DELETE FROM grouproles WHERE groupid="+groupId);
            stmt.execute("DELETE FROM groups WHERE groupid="+groupId);
            stmt.execute("COMMIT");
            stmt.close();
            conn=null;
            roles=null;
            type=null;
            boundRole=null;
            guild=null;
            groupId=null;
            groupName=null;

        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleGroup)) return false;
        RoleGroup roleGroup = (RoleGroup) o;
        return Objects.equals(guild, roleGroup.guild) &&
                Objects.equals(groupName, roleGroup.groupName);
    }

    public static RoleGroup findGroup(List<RoleGroup> list,String groupName)
    {
        for (RoleGroup group : list) {
            if(group.getGroupName().equals(groupName))
                return group;
        }
        return null;
    }

    private boolean memberHasRole(Member member,Long roleId)
    {
        List<Role> list = member.getRoles();
        Role role = member.getGuild().getRoleById(roleId);
        if(role!=null)
            if(list.contains(role))
                return true;
        return false;
    }

}
