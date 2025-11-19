package net.sivils.playerWorlds.database;

public record PlayerInfo(
  boolean dirty,
  String lastJoinWorlds,
  String lastJoinTimes
) {

  public PlayerInfo withLastJoinWorlds(String lastJoinWorlds) {
    return new PlayerInfo(true, lastJoinWorlds, lastJoinTimes);
  }

  public PlayerInfo withLastJoinTimes(String lastJoinTimes) {
    return new PlayerInfo(true, lastJoinWorlds, lastJoinTimes);
  }
}
