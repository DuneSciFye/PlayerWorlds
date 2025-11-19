package net.sivils.playerWorlds.database;

public record PlayerAccess(
  boolean dirty,
  String playerUsername,
  Boolean canJoin,
  Boolean bypassPassword,
  Boolean breakBlocks,
  Boolean placeBlocks,
  Boolean pickupItems) {

  public PlayerAccess withCanJoin(boolean canJoin) {
    return new PlayerAccess(true, playerUsername, canJoin, bypassPassword, breakBlocks, placeBlocks, pickupItems);
  }
  public PlayerAccess withBypassPassword(boolean bypassPassword) {
    return new PlayerAccess(true, playerUsername, canJoin, bypassPassword, breakBlocks, placeBlocks, pickupItems);
  }
  public PlayerAccess withBreakBlocks(boolean breakBlocks) {
    return new PlayerAccess(true, playerUsername, canJoin, bypassPassword, breakBlocks, placeBlocks, pickupItems);
  }
  public PlayerAccess withPlaceBlocks(boolean placeBlocks) {
    return new PlayerAccess(true, playerUsername, canJoin, bypassPassword, breakBlocks, placeBlocks, pickupItems);
  }
  public PlayerAccess withPickupItems(boolean pickupItems) {
    return new PlayerAccess(true, playerUsername, canJoin, bypassPassword, breakBlocks, placeBlocks, pickupItems);
  }

  public PlayerAccess updateAccess(String type, boolean value) {
    return switch (type) {
      case "can_join" -> this.withCanJoin(value);
      case "bypass_password" -> this.withBypassPassword(value);
      case "break_blocks" -> this.withBreakBlocks(value);
      case "place_blocks" -> this.withPlaceBlocks(value);
      case "pickup_items" -> this.withPickupItems(value);
      default -> this;
    };
  }

}
