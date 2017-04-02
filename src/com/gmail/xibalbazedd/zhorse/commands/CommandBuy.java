package com.gmail.xibalbazedd.zhorse.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;

import com.gmail.xibalbazedd.zhorse.ZHorse;
import com.gmail.xibalbazedd.zhorse.database.HorseRecord;
import com.gmail.xibalbazedd.zhorse.enums.LocaleEnum;

public class CommandBuy extends AbstractCommand {

	public CommandBuy(ZHorse zh, CommandSender s, String[] a) {
		super(zh, s, a);
		playerOnly = true;
		needTarget = false;
		if (isPlayer() && analyseArguments() && hasPermission() && isWorldEnabled()) {
			if (!idMode) {
				if (!targetMode) {
					if (isOnHorse(false)) {
						horse = (AbstractHorse) p.getVehicle();
						execute();
					}
				}
				else {
					sendCommandUsage();
				}
			}
			else {
				if (isRegistered(targetUUID, horseID)) {
					horse = zh.getHM().getHorse(targetUUID, Integer.parseInt(horseID));
					if (isHorseLoaded(true)) {
						execute();
					}
				}
			}
		}
	}

	private void execute() {
		String previousHorseName = zh.getDM().getHorseName(horse.getUniqueId());
		if (zh.getDM().isHorseForSale(horse.getUniqueId())) {
			targetUUID = zh.getDM().getOwnerUUID(horse.getUniqueId());
			if (!p.getUniqueId().equals(targetUUID)) {
				if (!hasReachedClaimsLimit(p.getUniqueId()) && craftHorseName(true) && zh.getEM().canAffordCommand(p, command)) {
					int price = zh.getDM().getSalePrice(horse.getUniqueId());
					if (zh.getEM().canAffordPayment(p, price)) {
						int horseID = zh.getDM().getNextHorseID(p.getUniqueId());
						boolean lock = zh.getCM().shouldLockOnClaim();
						boolean protect = zh.getCM().shouldProtectOnClaim();
						boolean share = zh.getCM().shouldShareOnClaim();
						HorseRecord horseRecord = new HorseRecord(horse.getUniqueId().toString(), p.getUniqueId().toString(), horseID, horseName, lock, protect, share, horse.getLocation());
						boolean success = zh.getDM().removeSale(horse.getUniqueId());
						success &= zh.getDM().removeHorse(horse.getUniqueId(), targetUUID);
						success &= zh.getDM().registerHorse(horseRecord);
						if (success) {
							applyHorseName(p.getUniqueId());
							if (displayConsole) {
								String buyerLanguage = zh.getDM().getPlayerLanguage(p.getUniqueId());
								String buyerCurrencySymbol = zh.getLM().getMessage(LocaleEnum.CURRENCY_SYMBOL.getIndex(), buyerLanguage, true);
								zh.getMM().sendMessageAmountHorseValue(s, LocaleEnum.HORSE_BOUGHT, price, horseName, buyerCurrencySymbol);
								if (isPlayerOnline(targetUUID, true)) {
									String sellerLanguage = zh.getDM().getPlayerLanguage(targetUUID);
									String sellerCurrencySymbol = zh.getLM().getMessage(LocaleEnum.CURRENCY_SYMBOL.getIndex(), sellerLanguage, true);
									CommandSender seller = (CommandSender) zh.getServer().getPlayer(targetUUID);
									zh.getMM().sendMessageAmountHorsePlayerValue(seller, LocaleEnum.HORSE_SOLD, price, previousHorseName, p.getName(), sellerCurrencySymbol);
								}
							}
							zh.getEM().payPlayer(p, targetUUID, price);
							zh.getEM().payCommand(p, command);
						}
					}
					
				}
			}
			else if (displayConsole) {
				zh.getMM().sendMessage(s, LocaleEnum.HORSE_ALREADY_CLAIMED);
			}
		}
		else if (displayConsole) {
			zh.getMM().sendMessageHorse(s, LocaleEnum.HORSE_NOT_FOR_SALE, previousHorseName);
		}
	}

}