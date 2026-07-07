package net.trduc.magicabilitiesfork.data;

public class PowerteamRequest {
    private final String teamName;
    private final String requester;
    private final String target;
    private final long ts;

    public PowerteamRequest(String teamName, String requester, String target, long ts) {
        this.teamName = teamName;
        this.requester = requester;
        this.target = target;
        this.ts = ts;
    }

    public String getTeamName() { return teamName; }
    public String getRequester() { return requester; }
    public String getTarget() { return target; }
    public long getTs() { return ts; }
}

