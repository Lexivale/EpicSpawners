package com.songoda.epicspawners.listeners;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epicspawners.EpicSpawners;
import com.songoda.epicspawners.spawners.events.SpawnerChangeEvent;
import com.songoda.epicspawners.spawners.object.Spawner;
import com.songoda.epicspawners.spawners.object.SpawnerStack;
import com.songoda.epicspawners.utils.Debugger;
import com.songoda.epicspawners.utils.Methods;
import com.songoda.epicspawners.utils.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.SpawnEgg;

/**
 * Created by songoda on 2/25/2017.
 */
public class InteractListeners implements Listener {

    private final EpicSpawners instance;

    public InteractListeners(EpicSpawners instance) {
        this.instance = instance;

    }

    @SuppressWarnings("ConstantConditions")
    @EventHandler(ignoreCancelled = true)
    public void PlayerInteractEventEgg(PlayerInteractEvent e) {
        try {

            if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                return;
            }
            Player p = e.getPlayer();
            Block b = e.getClickedBlock();
            ItemStack i = e.getItem();
            Material is = null;
            if (e.getItem() != null) {
                is = i.getType();
            }
            int radius = EpicSpawners.getInstance().getConfig().getInt("Main.Spawners Repel Liquid Radius");
            if (e.getItem() != null
                    && is.equals(Material.WATER_BUCKET)
                    && radius != 0) {
                Block block = e.getClickedBlock();
                int bx = block.getX();
                int by = block.getY();
                int bz = block.getZ();
                for (int fx = -radius; fx <= radius; fx++) {
                    for (int fy = -radius; fy <= radius; fy++) {
                        for (int fz = -radius; fz <= radius; fz++) {
                            Block b2 = e.getClickedBlock().getWorld().getBlockAt(bx + fx, by + fy, bz + fz);
                            if (b2.getType().equals(Material.MOB_SPAWNER)) {
                                e.setCancelled(true);
                            }
                        }
                    }
                }
            }

            if (e.getClickedBlock().getType() == Material.MOB_SPAWNER && is == Material.MONSTER_EGG && EpicSpawners.getInstance().getBlacklistHandler().isBlacklisted(p, true))
                e.setCancelled(true);
            if (!(e.getClickedBlock().getType() == Material.MOB_SPAWNER && is == Material.MONSTER_EGG && !EpicSpawners.getInstance().getBlacklistHandler().isBlacklisted(p, true))) {
                return;
            }
            Spawner spawner = instance.getSpawnerManager().getSpawnerFromWorld(b.getLocation());
            String btype = Methods.getType(spawner.getCreatureSpawner().getSpawnedType());

            if (!EpicSpawners.getInstance().getConfig().getBoolean("Main.Convert Spawners With Eggs")
                    || !EpicSpawners.getInstance().spawnerFile.getConfig().getBoolean("Entities." + btype + ".Allowed")) {  //ToDo When you redo eggs make it so that if you use one on an omni
                e.setCancelled(true);
                return;
            }

            int bmulti = 1;
            if (EpicSpawners.getInstance().dataFile.getConfig().getInt("data.spawner." + Arconix.pl().getApi().serialize().serializeLocation(b)) != 0)
                bmulti = EpicSpawners.getInstance().dataFile.getConfig().getInt("data.spawner." + Arconix.pl().getApi().serialize().serializeLocation(b));
            int amt = p.getInventory().getItemInHand().getAmount();
            EntityType itype;

            if (EpicSpawners.getInstance().v1_7 || EpicSpawners.getInstance().v1_8)
                itype = ((SpawnEgg) i.getData()).getSpawnedType();
            else {
                String str = Reflection.getNBTTagCompound(Reflection.getNMSItemStack(i)).toString();
                if (str.contains("minecraft:"))
                    itype = EntityType.fromName(str.substring(str.indexOf("minecraft:") + 10, str.indexOf("\"}")));
                else
                    itype = EntityType.fromName(str.substring(str.indexOf("EntityTag:{id:") + 15, str.indexOf("\"}")));
            }

            if (!p.hasPermission("epicspawners.egg." + itype) && !p.hasPermission("epicspawners.egg.*")) {
                return;
            }
            if (amt < bmulti) {
                p.sendMessage(instance.getLocale().getMessage("event.egg.needmore", bmulti));
                e.setCancelled(true);
                return;
            }
            SpawnerChangeEvent event = new SpawnerChangeEvent(b.getLocation(), p, btype, itype.name());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            if (btype.equals(Methods.getType(itype))) {
                p.sendMessage(instance.getLocale().getMessage("event.egg.sametype", btype));
                return;
            }
            spawner.getCreatureSpawner().setSpawnedType(itype);
            spawner.getCreatureSpawner().update();
            EpicSpawners.getInstance().getHologramHandler().processChange(b);
            if (p.getGameMode() != GameMode.CREATIVE) {
                Methods.takeItem(p, bmulti - 1);
            }
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void PlayerInteractEvent(PlayerInteractEvent e) {
        try {
            if (Methods.isOffhand(e)) return;

            Player player = e.getPlayer();
            Block block = e.getClickedBlock();
            Location location = block.getLocation();
            ItemStack item = e.getItem();

            if (block.getType() == Material.MOB_SPAWNER) {
                if (!instance.getSpawnerManager().isSpawner(location)) {
                    Spawner spawner = new Spawner(location);

                    spawner.addSpawnerType(new SpawnerStack(instance.getSpawnerManager().getSpawnerType(Methods.getType(spawner.getCreatureSpawner().getSpawnedType())), 1));
                    instance.getSpawnerManager().addSpawnerToWorld(location, spawner);
                }
            }

            String loc = Arconix.pl().getApi().serialize().serializeLocation(block);
            if (EpicSpawners.getInstance().dataFile.getConfig().getString("data.blockshop." + loc) != null) {
                e.setCancelled(true);
                EpicSpawners.getInstance().getShop().show(instance.getSpawnerManager().getSpawnerType(EpicSpawners.getInstance().dataFile.getConfig().getString("data.blockshop." + loc).toLowerCase()), 1, player);
                return;
            }

            if (!EpicSpawners.getInstance().getHookHandler().canBuild(e.getPlayer(), e.getClickedBlock().getLocation())
                    || e.getClickedBlock() == null
                    || e.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            Material is = null;
            if (e.getItem() != null) {
                is = item.getType();
            }
            if (is == Material.MONSTER_EGG)
                return;
            if (e.getClickedBlock().getType() == Material.MOB_SPAWNER && is == Material.MOB_SPAWNER && !EpicSpawners.getInstance().getBlacklistHandler().isBlacklisted(player, true)) {

                Spawner spawner = instance.getSpawnerManager().getSpawnerFromWorld(location);
                if (!player.isSneaking() && item.getItemMeta().getDisplayName() != null) {
                    String itype = EpicSpawners.getInstance().getApi().getIType(item);
                    if (player.hasPermission("epicspawners.stack." + itype) || player.hasPermission("epicspawners.stack.*")) {
                        spawner.stack(player, item);
                        instance.getHologramHandler().updateHologram(spawner);
                        e.setCancelled(true);
                    }
                }
            } else if (e.getClickedBlock().getType() == Material.MOB_SPAWNER && !EpicSpawners.getInstance().getBlacklistHandler().isBlacklisted(player, false)) {
                if (!player.isSneaking()) {
                    Spawner spawner = EpicSpawners.getInstance().getSpawnerManager().getSpawnerFromWorld(location);

                    spawner.overview(player, 1);
                    EpicSpawners.getInstance().getHologramHandler().processChange(block);
                    e.setCancelled(true);
                }
            }
        } catch (Exception ex) {
            Debugger.runReport(ex);
        }
    }
}