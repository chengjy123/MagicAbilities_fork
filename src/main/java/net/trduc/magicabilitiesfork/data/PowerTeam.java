package net.trduc.magicabilitiesfork.data;

import java.util.ArrayList;
import java.util.List;

public class PowerTeam {
    private final String name;
    private final String owner;
    private String color;
    private final List<String> members;

    public PowerTeam(String name, String owner, String color) {
        this.name = name;
        this.owner = owner;
        this.color = color;
        this.members = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getOwner() { return owner; }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<String> getMembers() {
        return members;
    }

    public void addMember(String player){
        if (!members.contains(player)) members.add(player);
    }

    public void removeMember(String player){
        members.remove(player);
    }
}

