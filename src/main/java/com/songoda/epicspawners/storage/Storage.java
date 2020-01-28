package com.songoda.epicspawners.storage;

import com.songoda.core.configuration.Config;
import com.songoda.epicspawners.EpicSpawners;
import com.songoda.epicspawners.boost.BoostData;
import com.songoda.epicspawners.player.PlayerData;
import com.songoda.epicspawners.settings.Settings;
import com.songoda.epicspawners.spawners.spawner.Spawner;
import com.songoda.epicspawners.spawners.spawner.SpawnerStack;
import com.songoda.epicspawners.utils.Methods;
import org.bukkit.Location;

import java.util.List;

public abstract class Storage {

    protected final EpicSpawners plugin;
    protected final Config dataFile;

    public Storage(EpicSpawners plugin) {
        this.plugin = plugin;
        this.dataFile = new Config(plugin, "data.yml");
        this.dataFile.load();
    }

    public abstract boolean containsGroup(String group);

    public abstract List<StorageRow> getRowsByGroup(String group);

    public abstract void prepareSaveItem(String group, StorageItem... items);

    public void updateData(EpicSpawners plugin) {
        // Save game data
        for (Spawner spawner : plugin.getSpawnerManager().getSpawners()) {
            if (spawner.getFirstStack() == null || spawner.getFirstStack().getSpawnerData() == null
                    || spawner.getSpawnerDataCount() == 1
                    && !spawner.getFirstStack().getSpawnerData().isCustom()
                    && spawner.getSpawnCount() == 0
                    && spawner.getPlacedBy() == null || !Settings.ALWAYS_REMEMBER_PLACER.getBoolean()) continue;

            Location spawnerLocation = spawner.getLocation();

            if (spawnerLocation == null || spawnerLocation.getWorld() == null) continue;

            StorageItem location = new StorageItem("location", Methods.serializeLocation(spawnerLocation));

            StringBuilder stacksStr = new StringBuilder();
            for (SpawnerStack stack : spawner.getSpawnerStacks()) {
                stacksStr.append(stack.getSpawnerData().getIdentifyingName()).append(":").append(stack.getStackSize()).append(";");
            }
            StorageItem stacks = new StorageItem("stacks", stacksStr.toString());

            StorageItem placedBy = spawner.getPlacedBy() != null ? new StorageItem("placedby", spawner.getPlacedBy().getUniqueId().toString()) : null;

            prepareSaveItem("spawners", location, stacks, new StorageItem("spawns", spawner.getSpawnCount()), placedBy);
        }

        for (BoostData boostData : plugin.getBoostManager().getBoosts()) {
            prepareSaveItem("boosts", new StorageItem("endtime", String.valueOf(boostData.getEndTime())),
                    new StorageItem("boosttype", boostData.getBoostType().name()),
                    new StorageItem("data", boostData.getData().toString()),
                    new StorageItem("amount", boostData.getAmtBoosted()));
        }

        for (PlayerData playerData : plugin.getPlayerActionManager().getRegisteredPlayers()) {
            prepareSaveItem("players", new StorageItem("uuid", playerData.getPlayer().getUniqueId().toString()),
                    new StorageItem("entitykills", playerData.getEntityKills()));
        }
    }

    public abstract void doSave();

    public abstract void save();

    public abstract void makeBackup();

    public abstract void closeConnection();

}
