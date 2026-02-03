package com.echochambers.listeners;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.EntityRemoveEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.Message;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

public class EntityRemoveListener implements Consumer<EntityRemoveEvent> {
    
    private static final HytaleLogger logger = HytaleLogger.get("EchoDriveCharger");
    // tate tested it, 15 works
    private static final double MAX_RANGE = 15.0;
    private static final double MAX_RANGE_SQUARED = MAX_RANGE * MAX_RANGE;
    
    // TODO add tier 11-15 when they drop (?)
    private static final String[] ECHO_DRIVES = {
        "Echo_Drive_Empty",
        "Echo_Drive_Charged_1",
        "Echo_Drive_Charged_2",
        "Echo_Drive_Charged_3",
        "Echo_Drive_Charged_4",
        "Echo_Drive_Charged_5",
        "Echo_Drive_Charged_6",
        "Echo_Drive_Charged_7",
        "Echo_Drive_Charged_8",
        "Echo_Drive_Charged_9",
        "Echo_Drive_Charged_10"
    };

    @Override
    public void accept(EntityRemoveEvent event) {
        try {
            Entity entity = event.getEntity();
            if (entity == null || entity instanceof Player) {
                return;
            }
            
            double[] entityPos = tryGetPosition(entity);
            
            Universe universe = Universe.get();
            if (universe == null) return;
            
            World world = universe.getDefaultWorld();
            if (world == null) return;
            
            List<Player> players = world.getPlayers();
            if (players.isEmpty())
                return;
            
            for (Player player : players) {
                if (player == null)
                    continue;
                
                Inventory inv = player.getInventory();
                if (inv == null) continue;
                
                ItemStack utilityItem = inv.getUtilityItem();
                if (utilityItem == null || utilityItem.isEmpty())
                    continue;
                
                String itemId = utilityItem.getItemId();
                int chargeLevel = findDriveLevel(itemId);
                
                if (chargeLevel < 0 || chargeLevel >= ECHO_DRIVES.length - 1) {
                    continue;
                }
                
                // check if player is close enough
                if (entityPos != null) {
                    double[] playerPos = tryGetPosition(player);
                    if (playerPos != null) {
                        double dx = playerPos[0] - entityPos[0];
                        double dy = playerPos[1] - entityPos[1];
                        double dz = playerPos[2] - entityPos[2];
                        double distSquared = dx*dx + dy*dy + dz*dz;
                        
                        if (distSquared > MAX_RANGE_SQUARED) {
                            continue;
                        }
                    }
                }
                
                if (upgradeEchoDrive(inv, chargeLevel)) {
                    int newLevel = chargeLevel + 1;
                    try {
                        player.getPlayerRef().sendMessage(
                            Message.raw("§aEcho Drive charged to " + newLevel + "/10")
                        );
                    } catch (Exception ignored) {}
                    
                    logger.at(Level.INFO).log("Charged echo drive for " + 
                        getPlayerName(player) + " to level " + newLevel);
                    return;
                }
            }
            
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error in entity remove handler: " + e.getMessage());
        }
    }
    
    private int findDriveLevel(String itemId) {
        for (int i = 0; i < ECHO_DRIVES.length; i++) {
            if (ECHO_DRIVES[i].equals(itemId))
                return i;
        }
        return -1;
    }
    
    private String getPlayerName(Player player) {
        try {
            return player.getPlayerRef().getUsername();
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    // had to use reflection, API doesn't expose position on removal
    // finally got this working... pain
    private double[] tryGetPosition(Entity entity) {
        try {
            // entity is being removed so try old position first
            try {
                Method m = entity.getClass().getMethod("getOldPosition");
                Object pos = m.invoke(entity);
                if (pos != null) {
                    double[] coords = extractCoords(pos);
                    if (coords != null) return coords;
                }
            } catch (Exception ignored) {}
            
            try {
                Method m = entity.getClass().getMethod("getPosition");
                Object pos = m.invoke(entity);
                if (pos != null) return extractCoords(pos);
            } catch (Exception ignored) {}
            
            // joseph added this
            try {
                Method m = entity.getClass().getMethod("getLocation");
                Object loc = m.invoke(entity);
                if (loc != null) return extractCoords(loc);
            } catch (Exception ignored) {}
            
            try {
                Method m = entity.getClass().getMethod("getTransformComponent");
                Object transform = m.invoke(entity);
                if (transform != null) {
                    Method pm = transform.getClass().getMethod("getPosition");
                    Object pos = pm.invoke(transform);
                    if (pos != null) return extractCoords(pos);
                }
            } catch (Exception ignored) {}
            
        } catch (Exception ignored) {}
        return null;
    }
    
    private double[] extractCoords(Object pos) {
        try {
            Method xm = pos.getClass().getMethod("getX");
            Method ym = pos.getClass().getMethod("getY");
            Method zm = pos.getClass().getMethod("getZ");
            double x = ((Number) xm.invoke(pos)).doubleValue();
            double y = ((Number) ym.invoke(pos)).doubleValue();
            double z = ((Number) zm.invoke(pos)).doubleValue();
            return new double[]{x, y, z};
        } catch (Exception e) {
            return null;
        }
    }
    
    // danny - took forever to figure out, API changed
    private boolean upgradeEchoDrive(Inventory inv, int currentLevel) {
        String nextDrive = ECHO_DRIVES[currentLevel + 1];
        ItemStack newStack = new ItemStack(nextDrive, 1);
        
        try {
            Method m = inv.getClass().getMethod("setUtilityItem", ItemStack.class);
            m.invoke(inv, newStack);
            return true;
        } catch (NoSuchMethodException e) {
            // doesnt exist, try other way
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to set utility item: " + e.getMessage());
            return false;
        }
        
        try {
            Method getUtil = inv.getClass().getMethod("getUtility");
            Object utilSection = getUtil.invoke(inv);
            if (utilSection != null) {
                // has to be short not int lol. spent 2 long on this
                Method setSlot = utilSection.getClass().getMethod("setItemStackForSlot", 
                    short.class, ItemStack.class);
                setSlot.invoke(utilSection, (short) 0, newStack);
                return true;
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to upgrade drive: " + e.getMessage());
        }
        
        return false;
    }
}
