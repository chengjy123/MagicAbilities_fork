package net.trduc.magicabilitiesfork.data;

import net.trduc.magicabilitiesfork.powers.PowerType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class DbManager {
    private Connection conn;
    private boolean dbReady = false;
    private final JavaPlugin plugin;
    public DbManager(JavaPlugin plugin){
        this.plugin=plugin;
    }
    public void init(){
        File file = new File(plugin.getDataFolder(), "data.db");
        if (!file.exists()){
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().severe("Could not create data.db: " + e.getMessage());
            }
        }
    }

    public boolean connect(){
        try{
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            Statement stmt = conn.createStatement();
            String sql = "create table if not exists powers (name TEXT PRIMARY KEY NOT NULL, power TEXT NOT NULL, enabled BOOLEAN NOT NULL, aura_enabled BOOLEAN NOT NULL DEFAULT 0);";
            stmt.execute(sql);
            String sql2 = "create table if not exists binds (name TEXT PRIMARY KEY NOT NULL," +
                    " ab0 INT NOT NULL, ab1 INT NOT NULL, ab2 INT NOT NULL, ab3 INT NOT NULL, ab4 INT NOT NULL, ab5 INT NOT NULL," +
                    " ab6 INT NOT NULL, ab7 INT NOT NULL, ab8 INT NOT NULL);";
            stmt.execute(sql2);

            String sql3 = "create table if not exists powerteams (name TEXT PRIMARY KEY NOT NULL, owner TEXT NOT NULL, color TEXT NOT NULL);";
            stmt.execute(sql3);
            String sql4 = "create table if not exists powerteam_members (team_name TEXT NOT NULL, player TEXT NOT NULL, PRIMARY KEY(team_name, player));";
            stmt.execute(sql4);
            String sql5 = "create table if not exists powerteam_requests (team_name TEXT NOT NULL, requester TEXT NOT NULL, target TEXT NOT NULL, ts INTEGER NOT NULL, PRIMARY KEY(team_name, requester, target));";
            stmt.execute(sql5);
            String sql6 = "create table if not exists powerteam_coowners (team_name TEXT NOT NULL, player TEXT NOT NULL, PRIMARY KEY(team_name, player));";
            stmt.execute(sql6);
            String sql7 = "create table if not exists powerteam_invites (team_name TEXT NOT NULL, inviter TEXT NOT NULL, target TEXT NOT NULL, ts INTEGER NOT NULL, PRIMARY KEY(team_name, target));";
            stmt.execute(sql7);
            migrateAuraColumn(stmt);
            migratePowerteamOwnerColumn(stmt);
            stmt.close();
            conn.close();
            dbReady = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void migrateAuraColumn(Statement stmt) {

        try {
            ResultSet rs = stmt.executeQuery("select aura_enabled from powers limit 1;");
            rs.close();
        } catch (Exception e) {
            try {
                stmt.execute("alter table powers add column aura_enabled BOOLEAN NOT NULL DEFAULT 0;");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void migratePowerteamOwnerColumn(Statement stmt) {
        try {

            ResultSet rs = stmt.executeQuery("select owner from powerteams limit 1;");
            rs.close();
        } catch (Exception e) {
            try {

                stmt.execute("alter table powerteams add column owner TEXT NOT NULL DEFAULT '';");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean isDbEnabled(){
        return dbReady;
    }

    public boolean isPlayerInDb(String name){
        boolean is = false;
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement stmt = conn.prepareStatement("select * from powers where name=?;");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()){
                is = true;
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return is;
    }

    public void addPlayer(String name, PowerType powerType){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            String insert = "insert into powers (name, power, enabled, aura_enabled) values" +
                    " (?, ?, 1, 1);";
            String insert2 = "insert into binds (name, ab0, ab1, ab2, ab3, ab4, ab5, ab6, ab7, ab8) values" +
                    " (?, 0, 1, 2, 3, 4, 5, 6, 7, 8);";

            PreparedStatement stmt = conn.prepareStatement(insert);
            stmt.setString(1, name);
            stmt.setString(2, powerType.toString());
            stmt.execute();
            stmt.close();
            PreparedStatement stmt2 = conn.prepareStatement(insert2);
            stmt2.setString(1, name);
            stmt2.execute();
            stmt2.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PlayerData getPlayerData(String playerName) {
        PlayerData pd = null;
        if (!isPlayerInDb(playerName)){
            addPlayer(playerName, PowerType.NONE);
        }
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement stmt = conn.prepareStatement("select * from powers where name=?;");
            stmt.setString(1, playerName);
            PreparedStatement stmt2 = conn.prepareStatement("select * from binds where name=?;");
            stmt2.setString(1, playerName);

            ResultSet rs = stmt.executeQuery();
            ResultSet rs2 = stmt2.executeQuery();
            HashMap<Integer, Integer> binds = new HashMap<>();
            for (int i = 0; i<9; i++){
                binds.put(i, rs2.getInt("ab"+i));
            }
            pd = new PlayerData(playerName, PowerType.valueOf(rs.getString("power")), binds, rs.getBoolean("enabled"), rs.getBoolean("aura_enabled"));

            stmt.close();
            stmt2.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pd;
    }

    public void updatePlayer(String name, PlayerData pd){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");

            String update = "update powers set power=?, enabled=?, aura_enabled=? where name=?;";
            PreparedStatement stmt = conn.prepareStatement(update);
            stmt.setString(1, pd.getPower().toString());
            stmt.setBoolean(2, pd.isEnabled());
            stmt.setBoolean(3, pd.isAuraEnabled());
            stmt.setString(4, name);
            stmt.execute();
            stmt.close();

            String sql = "update binds set ";
            for (int i = 0; i < 9; i++){
                if (i==8){
                    sql+="ab"+i+"="+pd.getBinds().get(i);
                } else {
                    sql+="ab"+i+"="+pd.getBinds().get(i)+", ";
                }
            }
            sql += " where name=?;";

            PreparedStatement stmt2 = conn.prepareStatement(sql);
            stmt2.setString(1, name);
            stmt2.execute();
            stmt2.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect(){
        try {
            conn.close();
            conn=null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean createPowerTeam(String teamName, String owner, String color){

        if (teamName == null || teamName.trim().isEmpty()) return false;
        teamName = teamName.replaceAll("[^A-Za-z0-9_-]", "_");
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement check = conn.prepareStatement("select name from powerteams where name=?;");
            check.setString(1, teamName);
            ResultSet rs = check.executeQuery();
            if (rs.next()){
                rs.close(); check.close(); conn.close(); return false;
            }
            rs.close();
            check.close();
            PreparedStatement insert = conn.prepareStatement("insert into powerteams (name, owner, color) values (?,?,?);");
            insert.setString(1, teamName);
            insert.setString(2, owner);
            insert.setString(3, color);
            insert.execute();
            insert.close();
            conn.close();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePowerTeam(String teamName){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement delMembers = conn.prepareStatement("delete from powerteam_members where team_name=?;");
            delMembers.setString(1, teamName);
            delMembers.execute();
            delMembers.close();
            PreparedStatement delTeam = conn.prepareStatement("delete from powerteams where name=?;");
            delTeam.setString(1, teamName);
            int rows = delTeam.executeUpdate();
            delTeam.close();
            conn.close();
            return rows > 0;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean addPlayerToTeam(String teamName, String playerName){

        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");

            PreparedStatement check = conn.prepareStatement("select name from powerteams where name=?;");
            check.setString(1, teamName);
            ResultSet rs = check.executeQuery();
            boolean teamExists = rs.next();
            rs.close();
            check.close();
            if (!teamExists) { conn.close(); return false; }

            PreparedStatement remOther = conn.prepareStatement("delete from powerteam_members where player=?;");
            remOther.setString(1, playerName);
            remOther.execute();
            remOther.close();

            PreparedStatement checkMember = conn.prepareStatement("select player from powerteam_members where team_name=? and player=?;");
            checkMember.setString(1, teamName);
            checkMember.setString(2, playerName);
            ResultSet rs2 = checkMember.executeQuery();
            boolean alreadyMember = rs2.next();
            rs2.close();
            checkMember.close();
            if (alreadyMember){ conn.close(); return false; }

            PreparedStatement insert = conn.prepareStatement("insert into powerteam_members (team_name, player) values (?,?);");
            insert.setString(1, teamName);
            insert.setString(2, playerName);
            insert.execute();
            insert.close();
            conn.close();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean addCoowner(String teamName, String playerName){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement ins = conn.prepareStatement("insert or ignore into powerteam_coowners (team_name, player) values (?,?);");
            ins.setString(1, teamName);
            ins.setString(2, playerName);
            ins.execute();
            ins.close();
            conn.close();
            return true;
        } catch (Exception e){ e.printStackTrace(); return false; }
    }

    public boolean removeCoowner(String teamName, String playerName){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement del = conn.prepareStatement("delete from powerteam_coowners where team_name=? and player=?;");
            del.setString(1, teamName);
            del.setString(2, playerName);
            int rows = del.executeUpdate();
            del.close();
            conn.close();
            return rows > 0;
        } catch (Exception e){ e.printStackTrace(); return false; }
    }

    public boolean isCoowner(String teamName, String playerName){
        boolean out = false;
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement stmt = conn.prepareStatement("select player from powerteam_coowners where team_name=? and player=? limit 1;");
            stmt.setString(1, teamName);
            stmt.setString(2, playerName);
            ResultSet rs = stmt.executeQuery();
            out = rs.next();
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e){ e.printStackTrace(); }
        return out;
    }

    public boolean createInvite(String teamName, String inviter, String target){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement exists = conn.prepareStatement("select target from powerteam_invites where team_name=? and target=? limit 1;");
            exists.setString(1, teamName);
            exists.setString(2, target);
            ResultSet er = exists.executeQuery();
            boolean alreadyInvited = er.next();
            er.close();
            exists.close();
            if (alreadyInvited){ conn.close(); return false; }
            PreparedStatement ins = conn.prepareStatement("insert into powerteam_invites (team_name, inviter, target, ts) values (?,?,?,?);");
            ins.setString(1, teamName);
            ins.setString(2, inviter);
            ins.setString(3, target);
            ins.setLong(4, System.currentTimeMillis());
            ins.execute(); ins.close(); conn.close(); return true;
        } catch (Exception e){ e.printStackTrace(); return false; }
    }

    public java.util.List<PowerteamRequest> listInvitesForPlayer(String playerName){
        java.util.List<PowerteamRequest> out = new java.util.ArrayList<>();
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement stmt = conn.prepareStatement("select team_name, inviter, ts from powerteam_invites where target=?;");
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                out.add(new PowerteamRequest(rs.getString("team_name"), rs.getString("inviter"), playerName, rs.getLong("ts")));
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e){ e.printStackTrace(); }
        return out;
    }

    public boolean acceptInvite(String teamName, String playerName){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement remOther = conn.prepareStatement("delete from powerteam_members where player=?;");
            remOther.setString(1, playerName); remOther.execute(); remOther.close();
            PreparedStatement ins = conn.prepareStatement("insert into powerteam_members (team_name, player) values (?,?);");
            ins.setString(1, teamName); ins.setString(2, playerName); ins.execute(); ins.close();
            PreparedStatement delInv = conn.prepareStatement("delete from powerteam_invites where team_name=? and target=?;");
            delInv.setString(1, teamName); delInv.setString(2, playerName); delInv.execute(); delInv.close();
            conn.close(); return true;
        } catch (Exception e){ e.printStackTrace(); return false; }
    }

    public boolean denyInvite(String teamName, String playerName){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement del = conn.prepareStatement("delete from powerteam_invites where team_name=? and target=?;");
            del.setString(1, teamName); del.setString(2, playerName); int rows = del.executeUpdate(); del.close(); conn.close(); return rows>0;
        } catch (Exception e){ e.printStackTrace(); return false; }
    }

    public boolean requestAddToTeam(String teamName, String requester, String playerName){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement check = conn.prepareStatement("select name from powerteams where name=?;");
            check.setString(1, teamName);
            ResultSet rs = check.executeQuery();
            boolean teamExists = rs.next();
            rs.close();
            check.close();
            if (!teamExists){ conn.close(); return false; }

            PreparedStatement exists = conn.prepareStatement("select * from powerteam_requests where team_name=? and target=?;");
            exists.setString(1, teamName);
            exists.setString(2, playerName);
            ResultSet er = exists.executeQuery();
            boolean alreadyRequested = er.next();
            er.close();
            exists.close();
            if (alreadyRequested){ conn.close(); return false; }

            PreparedStatement insert = conn.prepareStatement("insert into powerteam_requests (team_name, requester, target, ts) values (?,?,?,?);");
            insert.setString(1, teamName);
            insert.setString(2, requester);
            insert.setString(3, playerName);
            insert.setLong(4, System.currentTimeMillis());
            insert.execute();
            insert.close();
            conn.close();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean transferOwner(String teamName, String newOwner){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement check = conn.prepareStatement("select name from powerteams where name=?;");
            check.setString(1, teamName);
            ResultSet rs = check.executeQuery();
            boolean teamExists = rs.next();
            rs.close();
            check.close();
            if (!teamExists){ conn.close(); return false; }

            PreparedStatement update = conn.prepareStatement("update powerteams set owner=? where name=?;");
            update.setString(1, newOwner);
            update.setString(2, teamName);
            int rows = update.executeUpdate();
            update.close();

            if (rows > 0){

                addPlayerToTeam(teamName, newOwner);
            }
            conn.close();
            return rows > 0;
        } catch (Exception e){ e.printStackTrace(); return false; }
    }

    public java.util.List<PowerteamRequest> listRequestsForTeam(String teamName){
        java.util.List<PowerteamRequest> out = new java.util.ArrayList<>();
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement stmt = conn.prepareStatement("select requester, target, ts from powerteam_requests where team_name=?;");
            stmt.setString(1, teamName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                out.add(new PowerteamRequest(teamName, rs.getString("requester"), rs.getString("target"), rs.getLong("ts")));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e){ e.printStackTrace(); }
        return out;
    }

    public boolean approveRequest(String teamName, String target, String approver){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement ownerCheck = conn.prepareStatement("select owner from powerteams where name=? limit 1;");
            ownerCheck.setString(1, teamName);
            ResultSet ors = ownerCheck.executeQuery();
            if (!ors.next()){ ors.close(); ownerCheck.close(); conn.close(); return false; }
            String owner = ors.getString("owner");
            ors.close();
            ownerCheck.close();
            boolean isCo = isCoowner(teamName, approver);
            if (!owner.equals(approver) && !isCo) { conn.close(); return false; }

            PreparedStatement remOther = conn.prepareStatement("delete from powerteam_members where player=?;");
            remOther.setString(1, target);
            remOther.execute();
            remOther.close();

            PreparedStatement insert = conn.prepareStatement("insert into powerteam_members (team_name, player) values (?,?);");
            insert.setString(1, teamName);
            insert.setString(2, target);
            insert.execute();
            insert.close();

            PreparedStatement delReq = conn.prepareStatement("delete from powerteam_requests where team_name=? and target=?;");
            delReq.setString(1, teamName);
            delReq.setString(2, target);
            delReq.execute();
            delReq.close();
            conn.close();
            return true;
        } catch (Exception e){ e.printStackTrace(); return false; }
    }

    public boolean denyRequest(String teamName, String target, String approver){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement ownerCheck = conn.prepareStatement("select owner from powerteams where name=? limit 1;");
            ownerCheck.setString(1, teamName);
            ResultSet ors = ownerCheck.executeQuery();
            if (!ors.next()){ ors.close(); ownerCheck.close(); conn.close(); return false; }
            String owner = ors.getString("owner");
            ors.close();
            ownerCheck.close();
            boolean isCo = isCoowner(teamName, approver);
            if (!owner.equals(approver) && !isCo) { conn.close(); return false; }

            PreparedStatement delReq = conn.prepareStatement("delete from powerteam_requests where team_name=? and target=?;");
            delReq.setString(1, teamName);
            delReq.setString(2, target);
            int rows = delReq.executeUpdate();
            delReq.close();
            conn.close();
            return rows > 0;
        } catch (Exception e){ e.printStackTrace(); return false; }
    }

    public boolean removePlayerFromTeam(String teamName, String playerName){
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement del = conn.prepareStatement("delete from powerteam_members where team_name=? and player=?;");
            del.setString(1, teamName);
            del.setString(2, playerName);
            int rows = del.executeUpdate();
            del.close();
            conn.close();
            return rows > 0;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public String getPlayerTeam(String playerName){
        String team = null;
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement stmt = conn.prepareStatement("select team_name from powerteam_members where player=? limit 1;");
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()){
                team = rs.getString("team_name");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return team;
    }

    public PowerTeam getPowerTeam(String teamName){
        PowerTeam team = null;
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement stmt = conn.prepareStatement("select * from powerteams where name=?;");
            stmt.setString(1, teamName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()){
                team = new PowerTeam(rs.getString("name"), rs.getString("owner"), rs.getString("color"));
                rs.close();
                PreparedStatement mem = conn.prepareStatement("select player from powerteam_members where team_name=?;");
                mem.setString(1, teamName);
                ResultSet members = mem.executeQuery();
                while (members.next()){
                    team.addMember(members.getString("player"));
                }
                members.close();
                mem.close();
            } else {
                rs.close();
            }
            stmt.close();
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return team;
    }

    public List<String> listPowerTeams(){
        List<String> out = new ArrayList<>();
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement stmt = conn.prepareStatement("select name from powerteams;");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                out.add(rs.getString("name"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return out;
    }

    public List<String> listTeamsOwnedBy(String owner){
        List<String> out = new ArrayList<>();
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/"+plugin.getDataFolder().getName()+"/data.db");
            PreparedStatement stmt = conn.prepareStatement("select name from powerteams where owner=?;");
            stmt.setString(1, owner);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                out.add(rs.getString("name"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return out;
    }

}

