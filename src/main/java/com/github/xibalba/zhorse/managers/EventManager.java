package com.github.xibalba.zhorse.managers;

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.xibalba.zhorse.ZHorse;
import com.github.xibalba.zhorse.commands.CommandClaim;
import com.github.xibalba.zhorse.commands.CommandInfo;
import com.github.xibalba.zhorse.database.HorseDeathRecord;
import com.github.xibalba.zhorse.database.HorseInventoryRecord;
import com.github.xibalba.zhorse.database.HorseStatsRecord;
import com.github.xibalba.zhorse.enums.CommandEnum;
import com.github.xibalba.zhorse.enums.KeyWordEnum;
import com.github.xibalba.zhorse.enums.LocaleEnum;
import com.github.xibalba.zhorse.utils.ChunkLoad;
import com.github.xibalba.zhorse.utils.ChunkUnload;
import com.github.xibalba.zhorse.utils.MessageConfig;
import com.github.xibalba.zhorse.utils.PlayerJoin;
import com.github.xibalba.zhorse.utils.PlayerQuit;

public class EventManager implements Listener {

	private ZHorse zh;

	public EventManager(ZHorse zh) {
		this.zh = zh;
		zh.getServer().getPluginManager().registerEvents(this, zh);
	}

	public void unregisterEvents() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent e) {
		new ChunkLoad(zh, e.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent e) {
		new ChunkUnload(zh, e.getChunk());
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof AbstractHorse) {
			AbstractHorse horse = (AbstractHorse) e.getEntity();
			if (zh.getDM().isHorseRegistered(horse.getUniqueId())) {
				if (zh.getDM().isHorseProtected(horse.getUniqueId())) {
					DamageCause damageCause = e.getCause();
					if (!(damageCause.equals(DamageCause.ENTITY_ATTACK) // If not already handled by onEntityDamageByEntity
							|| damageCause.equals(DamageCause.ENTITY_EXPLOSION)
							|| damageCause.equals(DamageCause.ENTITY_SWEEP_ATTACK)
							|| damageCause.equals(DamageCause.PROJECTILE)
							|| damageCause.equals(DamageCause.MAGIC))) {
						if (zh.getCM().isProtectionEnabled(damageCause.name())) {
							e.setCancelled(true);
						}
					}
				}
				if (!e.isCancelled() && horse.getHealth() - e.getDamage() <= 0) {
					zh.getDM().updateHorseIsCarryingChest(horse.getUniqueId(), horse instanceof ChestedHorse ? ((ChestedHorse) horse).isCarryingChest() : false);
				}
			}
		}
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof AbstractHorse) {
			AbstractHorse horse = (AbstractHorse) e.getEntity();
			if (zh.getDM().isHorseRegistered(horse.getUniqueId())) {
				if (zh.getDM().isHorseProtected(horse.getUniqueId())) {
					if (e.getDamager() instanceof Player) {
						Player p = (Player) e.getDamager();
						e.setCancelled(!isPlayerAllowedToAttack(p, horse));
					}
					else if ((e.getDamager() instanceof Projectile) && ((Projectile) e.getDamager()).getShooter() instanceof Player) {
						Player p = (Player)((Projectile) e.getDamager()).getShooter();
						e.setCancelled(!isPlayerAllowedToAttack(p, horse));
					}
					else {
						e.setCancelled(zh.getCM().isProtectionEnabled(e.getCause().name()));
					}
				}
				if (!e.isCancelled() && horse.getHealth() - e.getDamage() <= 0) {
					zh.getDM().updateHorseIsCarryingChest(horse.getUniqueId(), horse instanceof ChestedHorse ? ((ChestedHorse) horse).isCarryingChest() : false);
				}
			}
		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {
		if (e.getEntity() instanceof AbstractHorse) {
			AbstractHorse horse = (AbstractHorse) e.getEntity();
			if (zh.getDM().isHorseRegistered(horse.getUniqueId())) {
				UUID ownerUUID = zh.getDM().getOwnerUUID(horse.getUniqueId());
				String horseName = zh.getDM().getHorseName(horse.getUniqueId());
				Player killer = e.getEntity().getKiller();
				if (killer != null && !killer.getUniqueId().equals(ownerUUID)) {
					zh.getMM().sendPendingMessage(ownerUUID, new MessageConfig(LocaleEnum.HORSE_KILLED) {{ setHorseName(horseName); setPlayerName(killer.getName());}});
				}
				else {
					zh.getMM().sendPendingMessage(ownerUUID, new MessageConfig(LocaleEnum.HORSE_DIED) {{ setHorseName(horseName); }});
				}
				HorseInventoryRecord inventoryRecord;
				if (zh.getCM().shouldRestoreInventory()) {
					e.getDrops().clear();
					inventoryRecord = new HorseInventoryRecord(horse);
				}
				else {
					inventoryRecord = new HorseInventoryRecord(horse.getUniqueId().toString());
				}
				e.setDroppedExp(0);
				zh.getHM().untrackHorse(horse.getUniqueId());
				/* isCarryingChest carried on before horse death as the chest is forcibly removed at the start of the event since Spigot 1.12 */
				/* Do not update stats to keep health and ticksLived above 0 */
				zh.getDM().updateHorseInventory(inventoryRecord);
				zh.getDM().registerHorseDeath(new HorseDeathRecord(horse.getUniqueId().toString()));
			}
		}
	}

	@EventHandler
	public void onEntityTame(EntityTameEvent e) {
		if (e.getOwner() instanceof Player && e.getEntity() instanceof AbstractHorse) {
			if (zh.getCM().shouldClaimOnTame()) {
				((AbstractHorse) e.getEntity()).setTamed(true);
				String[] a = {CommandEnum.CLAIM.name().toLowerCase()};
				new CommandClaim(zh, (CommandSender) e.getOwner(), a);
			}
			else if (zh.getPM().has((Player) e.getOwner(), KeyWordEnum.ZH_PREFIX.getValue() + CommandEnum.CLAIM.name().toLowerCase())) {
				zh.getMM().sendMessage((Player) e.getOwner(), new MessageConfig(LocaleEnum.HORSE_MANUALLY_TAMED));
			}
		}
	}

	@EventHandler
	public void onEntityPortal(EntityPortalEvent e) {
		if (e.getEntity() instanceof AbstractHorse) {
			AbstractHorse horse = (AbstractHorse) e.getEntity();
			if (zh.getDM().isHorseRegistered(horse.getUniqueId())) {
				e.setCancelled(true);
				Location destination = e.getTo();
				if (zh.getCM().isWorldCrossable(destination.getWorld())) {
					zh.getHM().teleportHorse(horse, destination);
				}
			}
		}
	}

	@EventHandler
	public void onEntityTeleport(EntityTeleportEvent e) {
		if (e.getEntity() instanceof AbstractHorse) {
			AbstractHorse horse = (AbstractHorse) e.getEntity();
			if (zh.getDM().isHorseRegistered(horse.getUniqueId())) {
				e.setCancelled(true);
				if (zh.getCM().isWorldEnabled(e.getTo().getWorld())) {
					zh.getHM().teleportHorse(horse, e.getTo());
				}
			}
		}
	}

	@EventHandler
	public void onHangingBreak(HangingBreakEvent e) {
		RemoveCause removeCause = e.getCause();
		if (!removeCause.equals(RemoveCause.ENTITY)) { // If not already handled by onHangingBreakByEntity
			if (e.getEntity() instanceof LeashHitch) {
				LeashHitch leashHitch = (LeashHitch) e.getEntity();
				for (AbstractHorse horse : zh.getHM().getTrackedHorses().values()) {
					if (horse.isLeashed()) {
						Entity leashHolder = horse.getLeashHolder();
						if (leashHitch.equals(leashHolder)) {
							e.setCancelled(zh.getDM().isHorseLocked(horse.getUniqueId()));
							break;
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onHangingBreakByEntity(HangingBreakByEntityEvent e) {
		if (e.getEntity() instanceof LeashHitch) {
			LeashHitch leashHitch = (LeashHitch) e.getEntity();
			for (AbstractHorse horse : zh.getHM().getTrackedHorses().values()) {
				if (horse.isLeashed()) {
					Entity leashHolder = horse.getLeashHolder();
					if (leashHitch.equals(leashHolder)) {
						if (e.getRemover() instanceof Player) {
							e.setCancelled(!isPlayerAllowedToInteract((Player) e.getRemover(), horse, false));
							break;
						}
						else {
							e.setCancelled(zh.getDM().isHorseLocked(horse.getUniqueId()));
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player && e.getInventory().getHolder() instanceof AbstractHorse) {
			e.setCancelled(!isPlayerAllowedToInteract((Player) e.getWhoClicked(), (AbstractHorse) e.getInventory().getHolder(), true));
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
		if (e.getRightClicked() instanceof AbstractHorse) {
			AbstractHorse horse = (AbstractHorse) e.getRightClicked();
			Player p = e.getPlayer();

			/* Manages stats display of horses for sale */
			if (zh.getDM().isHorseForSale(horse.getUniqueId()) && !zh.getDM().isHorseOwnedBy(p.getUniqueId(), horse.getUniqueId())) {
				displayHorseStats(horse, p);
			}

			/* Manages leashing of undead horses */
			if (horse instanceof SkeletonHorse || horse instanceof ZombieHorse) {
				if (!horse.isLeashed() && zh.getCM().isLeashOnUndeadHorseAllowed()) {
					HandEnum holdingHand = getHoldingHand(p, new ItemStack(Material.LEASH));
					if (!holdingHand.equals(HandEnum.NONE)) { // If player is holding a leash
						cancelEvent(e, p, true, true);
						PlayerLeashEntityEvent event = new PlayerLeashEntityEvent(horse, p, p);
						zh.getServer().getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							consumeItem(p, holdingHand);
							horse.setLeashHolder(p); // Does not work on untamed undead horses
						}
					}
				}
			}

			/* Manages riding of foals and untamed undead horses */
			boolean matchUseCase = false;
			boolean interactionAllowed = true;
			if (!horse.isAdult()) {
				matchUseCase = true;
				interactionAllowed &= zh.getCM().isFoalRidingAllowed();
			}
			if ((horse instanceof SkeletonHorse || horse instanceof ZombieHorse) && !horse.isTamed()) {
				matchUseCase = true;
				interactionAllowed &= zh.getCM().isTamingOfUndeadHorseAllowed();
			}
			if (matchUseCase && interactionAllowed && horse.getPassengers().isEmpty()) {
				if (!p.isSneaking() // Allows to give food, open inventory, put on leash or place chest
						&& !(horse.isLeashed() && horse.getLeashHolder().equals(p))
						&& getHoldingHand(p, new ItemStack(Material.LEASH)).equals(HandEnum.NONE)
						&& getHoldingHand(p, new ItemStack(Material.SADDLE)).equals(HandEnum.NONE)
						&& getHoldingHand(p, new ItemStack(Material.CARPET)).equals(HandEnum.NONE)
						&& getHoldingHand(p, new ItemStack(Material.IRON_BARDING)).equals(HandEnum.NONE)
						&& getHoldingHand(p, new ItemStack(Material.GOLD_BARDING)).equals(HandEnum.NONE)
						&& getHoldingHand(p, new ItemStack(Material.DIAMOND_BARDING)).equals(HandEnum.NONE)
						&& getHoldingHand(p, new ItemStack(Material.CHEST)).equals(HandEnum.NONE))
				{
					cancelEvent(e, p, true, true);
					VehicleEnterEvent event = new VehicleEnterEvent(horse, p);
					zh.getServer().getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						horse.addPassenger(p);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent e) {
		new PlayerJoin(zh, e.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerKick(PlayerKickEvent e) {
		new PlayerQuit(zh, e.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent e) {
		new PlayerQuit(zh, e.getPlayer());
	}

	@EventHandler
	public void onPlayerLeashEntity(PlayerLeashEntityEvent e) {
		if (e.getLeashHolder() instanceof Player && e.getEntity() instanceof AbstractHorse) {
			Player p = (Player) e.getPlayer();
			ItemStack item = getItem(p, getHoldingHand(p, new ItemStack(Material.LEASH)));
			int savedAmount = item.getAmount();
			e.setCancelled(!isPlayerAllowedToInteract(p, (AbstractHorse) e.getEntity(), false));
			if (e.isCancelled()) {
				item.setAmount(savedAmount);
				updateInventory(p);
			}
		}
	}

	@EventHandler
	public void onPlayerUnleashEntity(PlayerUnleashEntityEvent e) {
		if (e.getEntity() instanceof AbstractHorse) {
			e.setCancelled(!isPlayerAllowedToInteract(e.getPlayer(), (AbstractHorse) e.getEntity(), false));
		}
	}

	@EventHandler
	public void onVehicleEnter(VehicleEnterEvent e) {
		if (e.getEntered() instanceof Player && e.getVehicle() instanceof AbstractHorse) {
			Player p = (Player) e.getEntered();
			boolean cancel = !isPlayerAllowedToInteract(p, (AbstractHorse) e.getVehicle(), false);
			cancelEvent(e, p, cancel, true);
		}
	}

	private void cancelEvent(Cancellable e, Player p, boolean cancel, boolean restoreLocation) {
		Location savedLocation = p.getLocation();
		e.setCancelled(cancel);
		if (cancel && restoreLocation) {
			p.teleport(savedLocation);
		}
	}

	private void consumeItem(Player p, HandEnum holdingHand) {
		if (p.getGameMode() != GameMode.CREATIVE) {
			ItemStack item = getItem(p, holdingHand);
			if (item != null) {
				int amount = item.getAmount();
				if (amount > 1) {
					item.setAmount(amount - 1);
				}
				else {
					p.getInventory().remove(item);
				}
				updateInventory(p);
			}
		}
	}

	private void displayHorseStats(AbstractHorse horse, Player p) {
		HorseStatsRecord statsRecord = new HorseStatsRecord(horse);
		boolean useExactStats = zh.getCM().shouldUseExactStats();
		boolean useVanillaStats = zh.getCM().shouldUseVanillaStats();
		CommandInfo.displayInfoHeader(zh, (CommandSender) p);
		CommandInfo.displayHealth(zh, (CommandSender) p, statsRecord);
		CommandInfo.displaySpeed(zh, (CommandSender) p, statsRecord, useExactStats, useVanillaStats);
		CommandInfo.displayJumpStrength(zh, (CommandSender) p, statsRecord, useExactStats, useVanillaStats);
		CommandInfo.displayChestSize(zh, (CommandSender) p, horse, statsRecord);
		CommandInfo.displayPrice(zh, (CommandSender) p, horse);
	}

	private HandEnum getHoldingHand(Player p, ItemStack item) {
		ItemStack mainHandItem = p.getInventory().getItemInMainHand();
		ItemStack offHandItem = p.getInventory().getItemInOffHand();
		if (mainHandItem != null && mainHandItem.isSimilar(item)) {
			return HandEnum.MAIN;
		}
		else if (offHandItem != null && offHandItem.isSimilar(item)) {
			return HandEnum.OFF;
		}
		return HandEnum.NONE;
	}

	private ItemStack getItem(Player p, HandEnum holdingHand) {
		ItemStack item = null;
		switch (holdingHand) {
		case MAIN:
			item = p.getInventory().getItemInMainHand();
			break;
		case OFF:
			item = p.getInventory().getItemInOffHand();
			break;
		default:
			break;
		}
		return item;
	}

	private boolean isPlayerAllowedToAttack(Player p, AbstractHorse horse) {
		if (zh.getCM().isProtectionEnabled(CustomAttackType.PLAYER.getCode())) {
			boolean isOwner = zh.getDM().isHorseOwnedBy(p.getUniqueId(), horse.getUniqueId());
			boolean isOwnerAttackBlocked = zh.getCM().isProtectionEnabled(CustomAttackType.OWNER.getCode());
			boolean isFriend = zh.getDM().isFriendOfOwner(p.getUniqueId(), horse.getUniqueId());
			boolean hasAdminPerm = zh.getPM().has(p, KeyWordEnum.ZH_PREFIX.getValue() + CommandEnum.PROTECT.name().toLowerCase() + KeyWordEnum.ADMIN_SUFFIX.getValue());
			if ((!(isOwner || isFriend) || isOwnerAttackBlocked) && !hasAdminPerm) {
				String horseName = zh.getDM().getHorseName(horse.getUniqueId());
				zh.getMM().sendMessage(p, new MessageConfig(LocaleEnum.HORSE_IS_PROTECTED) {{ setHorseName(horseName); }});
				return false;
			}
		}
		return true;
	}

	private boolean isPlayerAllowedToInteract(Player p, AbstractHorse horse, boolean mustBeShared) {
		if (zh.getDM().isHorseRegistered(horse.getUniqueId())) {
			boolean isOwner = zh.getDM().isHorseOwnedBy(p.getUniqueId(), horse.getUniqueId());
			boolean isFriend = zh.getDM().isFriendOfOwner(p.getUniqueId(), horse.getUniqueId());
			boolean hasAdminPerm = zh.getPM().has(p, KeyWordEnum.ZH_PREFIX.getValue() + CommandEnum.LOCK.name().toLowerCase() + KeyWordEnum.ADMIN_SUFFIX.getValue());
			if (!isOwner && !isFriend && !hasAdminPerm) {
				if (zh.getDM().isHorseLocked(horse.getUniqueId()) || (!zh.getDM().isHorseShared(horse.getUniqueId()) && (!horse.isEmpty() || mustBeShared))) {
					String ownerName = zh.getDM().getOwnerName(horse.getUniqueId());
					zh.getMM().sendMessage(p, new MessageConfig(LocaleEnum.HORSE_BELONGS_TO) {{ setPlayerName(ownerName); }});
					return false;
				}
			}
		}
		return true;
	}

	private void updateInventory(Player p) {
		new BukkitRunnable() {

			@Override
			public void run() {
				p.updateInventory();
			}

		}.runTaskLater(zh, 0);
	}

	public enum HandEnum {

		MAIN,
		OFF,
		NONE;

	}

	public enum CustomAttackType {

		OWNER("OWNER_ATTACK"),
		PLAYER("PLAYER_ATTACK");

		String code;

		private CustomAttackType(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}

	}
}
