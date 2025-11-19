package net.sivils.playerWorlds.database;

import java.sql.Timestamp;

public record WorldData(
  boolean dirty,
  String worldUUID,
  String ownerUUID,
  String displayName,
  Timestamp creationTime,
  Timestamp lastUseTime,
  int deletionTime,
  Long seed,
  String password,
  boolean passwordEnabled,
  boolean cheatsEnabled,
  String plugins,
  PlayerAccess defaultPlayerAccess
) {

  public boolean canJoinWorld(PlayerAccess playerAccess) {
    // If no specific player access, then get the default player access for the world
    if (playerAccess == null) return defaultPlayerAccess().canJoin();
    else return playerAccess.canJoin();
  }

  public WorldData withDisplayName(String displayName) {
    return new WorldData(true, worldUUID, ownerUUID, displayName, creationTime, lastUseTime, deletionTime, seed,
      password, passwordEnabled, cheatsEnabled, plugins, defaultPlayerAccess);
  }

  public WorldData withPassword(String password) {
    return new WorldData(true, worldUUID, ownerUUID, displayName, creationTime, lastUseTime, deletionTime, seed,
      password, passwordEnabled, cheatsEnabled, plugins, defaultPlayerAccess);
  }

  public WorldData withPasswordEnabled(boolean passwordEnabled) {
    return new WorldData(true, worldUUID, ownerUUID, displayName, creationTime, lastUseTime, deletionTime, seed,
      password, passwordEnabled, cheatsEnabled, plugins, defaultPlayerAccess);
  }

  public WorldData withCheatsEnabled(boolean cheatsEnabled) {
    return new WorldData(true, worldUUID, ownerUUID, displayName, creationTime, lastUseTime, deletionTime, seed,
      password, passwordEnabled, cheatsEnabled, plugins, defaultPlayerAccess);
  }

  public WorldData withDefaultPlayerAccess(PlayerAccess defaultPlayerAccess) {
    return new WorldData(true, worldUUID, ownerUUID, displayName, creationTime, lastUseTime, deletionTime, seed,
      password, passwordEnabled, cheatsEnabled, plugins, defaultPlayerAccess);
  }
}
