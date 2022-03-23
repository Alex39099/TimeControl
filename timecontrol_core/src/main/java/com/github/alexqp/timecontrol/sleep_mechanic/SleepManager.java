package com.github.alexqp.timecontrol.sleep_mechanic;

import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import com.github.alexqp.timecontrol.data.WorldContainer;
import com.github.alexqp.timecontrol.main.InternalsProvider;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;

public class SleepManager {

    public static SleepManager build(@NotNull JavaPlugin plugin, @NotNull InternalsProvider internals, @NotNull WorldContainer container) {
        return new SleepManager(Objects.requireNonNull(plugin), Objects.requireNonNull(internals), Objects.requireNonNull(container));
    }

    private final JavaPlugin plugin;
    private final WorldContainer worldContainer;

    private final HashSet<String> softSleepingWorlds = new HashSet<>();
    private final HashSet<String> hardSleepingWorlds = new HashSet<>(); // i.e. worlds were everyone slept (and got kicked out of bed). Only used by AllLeftBedTrigger

    private final SleepObserver sleepObserver;
    private final SleepMessenger sleepMessenger;

    private final int worldUpdateDelay = 10;
    private final HashSet<String> onWorldUpdateCooldown = new HashSet<>();

    private SleepManager(@NotNull JavaPlugin plugin, @NotNull InternalsProvider internals, @NotNull WorldContainer worldContainer) {
        this.plugin = plugin;
        this.worldContainer = worldContainer;

        this.sleepObserver = SleepObserver.build(plugin, this);
        this.sleepMessenger = SleepMessenger.build(plugin);

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            internals.disableSleepActionBar(plugin);
        } else {
            ConsoleMessage.send(ConsoleErrorType.WARN, plugin, "ProtocolLib is not installed on your server. Vanilla-Sleep-Messages will not be disabled");
        }

        if (internals.needAllLeftBedTrigger())
            Bukkit.getPluginManager().registerEvents(new AllLeftBedTrigger(plugin, this, worldUpdateDelay / 2), plugin);
    }

    public boolean isSleepObserved(@Nullable World world) {
        //noinspection ConstantConditions
        return worldContainer.isLoaded(world) && worldContainer.getTimeWorldForWorld(world).getNeededSleepPercentage() > 0;
    }

    public boolean isSleepingWorld(@Nullable World world) {
        return world != null && (hardSleepingWorlds.contains(world.getName()) || softSleepingWorlds.contains(world.getName()));
    }

    private boolean isNotHardSleepingWorld(@Nullable World world) {
        return world == null || !hardSleepingWorlds.contains(world.getName());
    }

    private void addSoftSleepingWorld(@Nullable World world) {
        if (this.isSleepObserved(world)) {
            assert world != null;
            softSleepingWorlds.add(world.getName());
        }
    }

    private void removeSoftSleepingWorld(@Nullable World world) {
        if (this.isSleepObserved(world)) {
            assert world != null;
            softSleepingWorlds.remove(world.getName());
        }
    }

    public void setSleeping(@Nullable World world, boolean sleeping) {
        ConsoleMessage.debug(this.getClass(), plugin, "Changing sleep states...");
        this.setHardSleeping(world, sleeping);
        this.setSoftSleeping(world, sleeping);
        if (!sleeping)
            sleepMessenger.setMsgCooldown(world, worldUpdateDelay);
    }

    private void setSoftSleeping(@Nullable World world, boolean sleeping) {
        if (this.isSleepObserved(world)) {
            assert world != null;
            if (sleeping) {
                this.addSoftSleepingWorld(world);
                ConsoleMessage.debug(this.getClass(), plugin, "World " + world.getName() + " is now soft sleeping.");
            } else {
                this.removeSoftSleepingWorld(world);
                ConsoleMessage.debug(this.getClass(), plugin, "World " + world.getName() + " is no longer soft sleeping.");
            }
        }
    }

    public void setHardSleeping(@Nullable World world, boolean sleeping) {
        if (this.isSleepObserved(world)) {
            assert world != null;
            if (sleeping) {
                this.hardSleepingWorlds.add(world.getName());
                ConsoleMessage.debug(this.getClass(), plugin, "World " + world.getName() + " is now hard sleeping.");
                sleepMessenger.sendSkipping(world);
            } else {
                this.hardSleepingWorlds.remove(world.getName());
                ConsoleMessage.debug(this.getClass(), plugin, "World " + world.getName() + " is no longer hard sleeping.");
            }
        }
    }

    private int getNeeded(@Nullable World world) throws IllegalArgumentException {
        if (this.isSleepObserved(world)) {
            assert world != null;
            //noinspection ConstantConditions
            return Math.max(1, (int) Math.ceil(sleepObserver.getNotSleeping(world) * worldContainer.getTimeWorldForWorld(world).getNeededSleepPercentage()));
        } else {
            throw new IllegalArgumentException("world is not sleep observed");
        }
    }

    private int getSleeping(@Nullable World world) throws IllegalArgumentException {
        if (this.isSleepObserved(world)) {
            assert world != null;
            return sleepObserver.getSleeping(world);
        } else {
            throw new IllegalArgumentException("world is not sleep observed");
        }
    }

    public void addSleepingPlayer(@NotNull Player p) {
        World world = p.getWorld();
        ConsoleMessage.debug(this.getClass(), plugin, "Adding sleeping player to world " + world.getName() + "...");
        if (this.isSleepObserved(world)) {
            if (!this.isSleepingWorld(world)) {
                int needed = getNeeded(world);
                int sleeping = getSleeping(world);
                sleepMessenger.sendBedEnterMsg(p, needed, sleeping);
                this.updateWorld(world, needed, sleeping);
            }
        }
    }

    public void removeSleepingPlayer(@NotNull Player p) {
        World world = p.getWorld();
        ConsoleMessage.debug(this.getClass(), plugin, "Removing sleeping player from world " + world.getName() + "...");
        if (this.isSleepObserved(world)) {
            if (this.isNotHardSleepingWorld(world)) {
                if (this.onWorldUpdateCooldown.add(world.getName())) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (isNotHardSleepingWorld(world)) {
                                int needed = getNeeded(world);
                                int sleeping = getSleeping(world);
                                sleepMessenger.sendBedLeaveMsg(p, needed, sleeping);
                                updateWorld(world, needed, sleeping);
                            }
                            onWorldUpdateCooldown.remove(world.getName());
                        }
                    }.runTaskLater(plugin, worldUpdateDelay);
                }
            }
        }
    }

    public void updateWorld(@NotNull World world, int needed, int sleeping) {
        ConsoleMessage.debug(this.getClass(), plugin, "Updating world " + world.getName() + "...");
        if (this.isSleepObserved(world) && this.isNotHardSleepingWorld(world)) {
            if (needed <= sleeping) {
                sleepMessenger.sendSkipping(world);
                this.setSoftSleeping(world, true);
            } else {
                if (sleeping > 1)
                    sleepMessenger.sendProgress(world, needed, sleeping);
                this.setSoftSleeping(world, false);
            }
        }
    }

    public void updateWorld(@Nullable World world) {
        if (this.isSleepObserved(world)) {
            assert world != null;
            int needed = this.getNeeded(world);
            int sleeping = this.getSleeping(world);
            this.updateWorld(world, needed, sleeping);
        }
    }

    public void setMessengerPrefix(@NotNull BaseComponent prefix) {
        sleepMessenger.setPrefix(prefix);
    }

}
