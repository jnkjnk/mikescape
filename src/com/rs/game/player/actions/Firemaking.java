package com.rs.game.player.actions;

import com.rs.game.Animation;
import com.rs.game.World;
import com.rs.game.WorldObject;
import com.rs.game.WorldTile;
import com.rs.game.item.FloorItem;
import com.rs.game.item.Item;
import com.rs.game.npc.familiar.Familiar;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.net.decoders.handlers.InventoryOptionsHandler;
import com.rs.utils.Utils;

public class Firemaking extends Action {

	public static enum Fire {
		//NAME(logs,level,?,fire,xp,?)
		NORMAL(1511, 1, 30, 2732, 2, 20), ACHEY(2862, 1, 30, 2732, 2, 1),
		OAK(1521, 15, 45, 2732, 4, 1), WILLOW(1519, 30, 45, 2732, 11, 1), 
		TEAK(6333, 35, 45, 2732, 8, 1), ARCTIC_PINE(10810, 42, 50, 2732,8, 1),
		MAPLE(1517, 45, 50, 2732, 11, 1), MAHOGANY(6332, 50,70, 2732, 157.5, 1),
		EUCALYPTUS(12581, 58, 70, 2732, 193.5, 1), YEW(1515, 60, 80, 2732, 16, 1), 
		MAGIC(1513, 75, 90, 2732, 60,1), CURSED_MAGIC(13567, 82, 100, 2732, 80, 1);

		private int logId;
		private int level;
		private int life;
		private int fireId;
		private int time;
		private double xp;

		Fire(int logId, int level, int life, int fireId, double xp, int time) {
			this.logId = logId;
			this.level = level;
			this.life = life;
			this.fireId = fireId;
			this.xp = xp;
			this.time = time;
		}

		public int getLogId() {
			return logId;
		}

		public int getLevel() {
			return level;
		}

		public int getLife() {
			return (life * 600);
		}

		public int getFireId() {
			return fireId;
		}

		public double getExperience() {
			return xp;
		}

		public int getTime() {
			return time;
		}
	}

	private Fire fire;

	public Firemaking(Fire fire) {
		this.fire = fire;
	}

	@Override
	public boolean start(Player player) {
		if (!checkAll(player))
			return false;
		player.getPackets().sendGameMessage("You attempt to light the logs.",
				true);
		player.getInventory().deleteItem(fire.getLogId(), 1);
		World.addGroundItem(new Item(fire.getLogId(), 1),
				new WorldTile(player), player, false, 180, true);
		Long time = (Long) player.getTemporaryAttributtes().remove("Fire");
		boolean quickFire = time != null && time > Utils.currentTimeMillis();
		setActionDelay(player, quickFire ? 1 : Utils.getRandom(5) + 4);
		if (!quickFire)
			player.setNextAnimation(new Animation(733));
		player.getPackets().sendSound(2599, 0, 1);
		return true;
	}

	public static boolean isFiremaking(Player player, Item item1, Item item2) {
		Item log = InventoryOptionsHandler.contains(590, item1, item2);
		if (log == null)
			return false;
		return isFiremaking(player, log.getId());
	}

	public static boolean isFiremaking(Player player, int logId) {
		for (Fire fire : Fire.values()) {
			if (fire.getLogId() == logId) {
				player.getActionManager().setSkill(new Firemaking(fire));
				return true;
			}
		}
		return false;

	}

	public static void startFamiliarFire(Player player, Familiar familiar,
			Fire fire) {
		if (player.getFamiliar().getId() == 7378
				|| player.getFamiliar().getId() == 7377) {
		}
	}

	public boolean checkAll(Player player) {
		if (!player.getInventory().containsItem(590, 1)) {
			player.getPackets().sendGameMessage(
					"You do not have the required items to light this.");
			return false;
		}
		if (player.getSkills().getLevel(Skills.FIREMAKING) < fire.getLevel()) {
			player.getPackets().sendGameMessage(
					"You do not have the required level to light this.");
			return false;
		}
		if (!World.canMoveNPC(player.getPlane(), player.getX(), player.getY(),
				1) // cliped
				|| World.getRegion(player.getRegionId()).getSpawnedObject(
						player) != null) { // contains object
			player.getPackets().sendGameMessage("You can't light a fire here.");
			return false;
		}
		return true;
	}

	@Override
	public boolean process(Player player) {
		return checkAll(player);
	}

	@Override
	public int processWithDelay(final Player player) {
		final WorldTile tile = new WorldTile(player);
		if (!player.addWalkSteps(player.getX() - 1, player.getY(), 1))
			if (!player.addWalkSteps(player.getX() + 1, player.getY(), 1))
				if (!player.addWalkSteps(player.getX(), player.getY() + 1, 1))
					player.addWalkSteps(player.getX(), player.getY() - 1, 1);
		player.getPackets().sendGameMessage(
				"The fire catches and the logs begin to burn.", true);
		WorldTasksManager.schedule(new WorldTask() {
			@Override
			public void run() {
				final FloorItem item = World.getRegion(tile.getRegionId())
						.getGroundItem(fire.getLogId(), tile, player);
				if (item == null)
					return;
				if (!World.removeGroundItem(player, item, false))
					return;
				World.spawnTempGroundObject(new WorldObject(fire.getFireId(),
						10, 0, tile.getX(), tile.getY(), tile.getPlane()), 592,
						fire.getLife());
				player.getPackets().sendSound(2594, 0, 1);
				player.getSkills().addXp(Skills.FIREMAKING,
						fire.getExperience());
				player.setNextFaceWorldTile(tile);
			}
		}, 1);
		player.getTemporaryAttributtes().put("Fire",
				Utils.currentTimeMillis() + 1800);
		return -1;
	}

	@Override
	public void stop(Player player) {

	}

}
