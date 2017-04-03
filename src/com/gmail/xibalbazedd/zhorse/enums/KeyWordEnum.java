package com.gmail.xibalbazedd.zhorse.enums;

public enum KeyWordEnum {
	
	/* general keywords */
	ADMIN_SUFFIX(".admin"),
	DOT("."),
	FREE_SUFFIX(".free"),
	ZH_PREFIX("zh."),
	
	/* config.yml keywords */
	ALLOW_LEASH_ON_UNDEAD_HORSE("Settings.allow-leash-on-undead-horse"),
	AVAILABLE_LANGUAGES("Languages.available"),
	AUTO_ADMIN_SUFFIX(".auto-admin"),
	BANNED_NAMES("HorseNames.banned-names"),
	BLOCK_LEASHED_TELEPORT("Settings.block-leashed-teleport"),
	BLOCK_MOUNTED_TELEPORT("Settings.block-mounted-teleport"),
	CLAIMS_LIMIT_SUFFIX(".claims-limit"),
	CLAIM_ON_TAME("Settings.claim-on-tame"),
	COLOR_BYPASS_SUFFIX(".color-bypass"),
	COLOR_SUFFIX(".color"),
	COMMANDS_PREFIX("Commands."),
	COST_SUFFIX(".cost"),
	CROSSABLE_SUFFIX(".crossable"),
	DATABASE("Databases.mysql-config.database"),
	DEFAULT_LANGUAGE("Languages.default"),
	DEFAULT_NAME("HorseNames.default-NAME"),
	ENABLED_SUFFIX(".enabled"),
	FILENAME("Databases.sqlite-config.filename"),
	GIVE_RANDOM_NAMES("HorseNames.give-random-names"),
	GROUPS("Groups"),
	GROUPS_PREFIX("Groups."),
	HERE_MAX_RANGE("Settings.here-max-range"),
	HOST("Databases.mysql-config.host"),
	LOCK_ON_CLAIM("Settings.lock-on-claim"),
	MAXIMUM_LENGTH("HorseNames.maximum-length"),
	MINIMUM_LENGTH("HorseNames.minimum-length"),
	MUTE_CONSOLE("Settings.mute-console"),
	PASSWORD("Databases.mysql-config.password"),
	PERMISSION_SUFFIX(".permission"),
	PORT("Databases.mysql-config.port"),
	PROTECTIONS_PREFIX("Protections."),
	PROTECT_ON_CLAIM("Settings.protect-on-claim"),
	RANDOM_NAMES("HorseNames.random-names"),
	RESPAWN_MISSING_HORSE("Settings.respawn-missing-horse"),
	SHARE_ON_CLAIM("Settings.share-on-claim"),
	TABLE_PREFIX("Databases.mysql-config.table-prefix"),
	TP_MAX_RANGE("Settings.tp-max-range"),
	TYPE("Databases.type"),
	USE_OLD_TELEPORT_METHOD("Settings.use-old-teleport-method"),
	USER("Databases.mysql-config.user"),
	USE_VANILLA_STATS("Settings.use-vanilla-stats"),
	WORLDS_PREFIX("Worlds."),
	
	/* locale.yml keywords */
	AMOUNT_FLAG("<amount>"),
	CURRENCY_FLAG("<currency>"),
	HORSE_FLAG("<horse>"),
	HORSE_ID_FLAG("<id>"),
	LANG_FLAG("<lang>"),
	MAX_FLAG("<max>"),
	PERM_FLAG("<perm>"),
	PLAYER_FLAG("<player>"),
	VALUE_FLAG("<value>"),
	
	/* LocaleEnum keywords */
	DESCRIPTION("DESCRIPTION"),
	SEPARATOR("_"),
	USAGE("USAGE");
	
	private String value;
	
	KeyWordEnum(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
}
