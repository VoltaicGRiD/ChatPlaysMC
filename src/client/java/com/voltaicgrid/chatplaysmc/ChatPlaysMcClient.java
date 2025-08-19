package com.voltaicgrid.chatplaysmc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.eventsub.*;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.item.Item;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.FluidTags;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import java.util.List;
import java.util.Optional;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import com.voltaicgrid.chatplaysmc.client.LockOnManager;
import com.voltaicgrid.chatplaysmc.mixin.HandledScreenAccessor;
import com.voltaicgrid.chatplaysmc.mixin.RecipeScreenAccessor;
import com.voltaicgrid.chatplaysmc.mixin.RecipeBookWidgetAccessor;
import com.voltaicgrid.chatplaysmc.mixin.RecipeBookResultsAccessor;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.client.gui.screen.*;

public class ChatPlaysMcClient implements ClientModInitializer {
    private static KeyBinding lookUpKey;
    private static KeyBinding lookDownKey;
    private static KeyBinding lookLeftKey;
    private static KeyBinding lookRightKey;

    private static TwitchClient twitchClient;
    
    // Queued actions driven by client ticks (avoid thread/sleep issues)
    private static int queuedBreakBlocks = 0;
    private static int queuedPlaceBlocks = 0;
    private static BlockPos breakingPos = null;
    private static Direction breakingSide = null;
    private static boolean buildingTower = false;
    private static boolean buildingBridge = false;
    private static boolean buildingMine = false;
    private static boolean buildingShaft = false;
    private static int brokenBlockIndex = 0; // for buildingMine

    private static double moveDistanceRemaining = 0.0;
    private static int moveDirection = -1; // 0 fwd, 1 back, 2 left, 3 right
    private static Vec3d prevPos = null;
    private static int moveStuckTicks = 0;
    private static int useTicksRemaining = 0;
    // Toggle states for sprint/sneak
    private static boolean sprintToggle = false;
    private static boolean sneakToggle = false;
    private static boolean isSwimming = false;
    private static int swimExitBuffer = 0; // Buffer to prevent immediate swim cancellation

    public static final String MODID = "chat_plays_mc";
    
    @Override
    public void onInitializeClient() {
        // Register the food satchel screen
        
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
        	
        	twitchClient = TwitchClientBuilder.builder()
            		.withEnableChat(true)
            		.build();
        	
        	twitchClient.getChat().joinChannel("oridont");

        	twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, event -> {
                final Matcher movementMatcher = Pattern.compile("\\b(w|s|a|d|forward|backward|left|right|jump|sprint|sneak)\\s*(\\d+(?:\\.\\d+)?)?\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher lookMatcher = Pattern.compile("\\b(lu|ld|ll|lr|look\\s+up|look\\s+down|look\\s+left|look\\s+right)\\s*(\\d+(?:\\.\\d+)?)?\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher clickMatcher = Pattern.compile("\\b(rc|lc|dc|right\\s+click|left\\s+click|double\\s*click)\\s*(\\d+(?:\\.\\d+)?)?\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher screenMatcher = Pattern.compile("\\b(inv|inventory|close|exit)\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher slotMatcher = Pattern.compile("\\b(slot)\\s*(\\d+)\\s*(all|move|craft|throw)?\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher lockonMatcher = Pattern.compile("\\b(lock)\\s*(on|off)\\s*(\\w*)\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher targetMatcher = Pattern.compile("\\b(target|lock)\\s*(next|previous|prev|nearest)\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher toggleMatcher = Pattern.compile("\\b(toggle)\\s*(attack|break|place)\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher resetMatcher = Pattern.compile("\\breset\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                // Add craft-by-name matcher (supports minecraft:id or plain name)
                final Matcher craftByNameMatcher = Pattern.compile("\\bcraft\\s+([a-z0-9_:\\-]+)(?:\\s+(all|max|one|1|\\d+))?\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher craftIndexMatcher = Pattern.compile("\\bcraft\\s+(\\d+)\\s*(all|max)?\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher rbSearchMatcher = Pattern.compile("\\brb\\s+search\\s+(.+)", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher swimMatcher = Pattern.compile("\\b(swim|dive)\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher extraMatcher = Pattern.compile("\\b(f3|swap|fov)\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                final Matcher buildMatcher = Pattern.compile("\\b(build)\\s*(tower|mine|shaft)\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(event.getMessage());
                
                // Debug: Print the message to see what we're processing
                System.out.println("Processing message: " + event.getMessage());
            	
                if (buildMatcher.find()) {
                	String cmd = buildMatcher.group(2).toLowerCase();
                	int count = Integer.parseInt(buildMatcher.group(3));
                	
                	if (cmd.equals("tower")) {
                		// Start building a tower: queue placing blocks while looking straight down
    					client.execute(() -> {
    						if (client.player != null) {
    							// Look straight down
    							client.player.setPitch(90.0f);
    							// Start moving up and queue placing blocks
    							queuedPlaceBlocks = count;
    							buildingTower = true;
    						}
    					});
    				} else if (cmd.equals("bridge")) {
    					// Start building a bridge: queue placing blocks while moving forward

    					client.execute(() -> {
    						if (client.player != null) {
    							// Level pitch
    							client.player.setPitch(0.0f);
    							// Start moving forward and queue placing blocks
    							queuedPlaceBlocks = count;
    							buildingBridge = true;
    						}
    					});
                	} else if (cmd.equals("mine")) {
    					// Set pitch to 0, and set yaw to nearest 90 degree (N/E/S/W) to dig straight

    					client.execute(() -> {
    						if (client.player != null) {
    							// Level pitch
    							client.player.setPitch(0.0f);
    							// Snap yaw to nearest 90 degrees
    							float yaw = client.player.getYaw();
    							float snappedYaw = MathHelper.wrapDegrees(Math.round(yaw / 90.0f) * 90.0f);
    							client.player.setYaw(snappedYaw);
    							
    							queuedBreakBlocks = count;
    							buildingMine = true;
    					        brokenBlockIndex = 0;
    						}
    					});
    				} else if (cmd.equals("shaft")) {

    					client.execute(() -> {
    						if (client.player != null) {
    							// Level pitch
    							client.player.setPitch(0.0f);
    							// Snap yaw to nearest 90 degrees
    							float yaw = client.player.getYaw();
    							float snappedYaw = MathHelper.wrapDegrees(Math.round(yaw / 90.0f) * 90.0f);
    							client.player.setYaw(snappedYaw);
//    							// Move to front of the block we're standing on
//    							Vec3d pos = client.player.getPos();
//    							client.player.updatePosition(pos.x + MathHelper.sin(-snappedYaw * ((float)Math.PI / 180F)) * 0.5, pos.y, pos.z + MathHelper.cos(snappedYaw * ((float)Math.PI / 180F)) * 0.5);
    							
    							queuedBreakBlocks = count;
    							buildingShaft = true;
    					        brokenBlockIndex = 0;
    						}
    					});
    				}
                }
                
                if (extraMatcher.find()) {
                	String cmd = extraMatcher.group(1).toLowerCase();

                	if (cmd.equals("f3")) {
//        				client.execute(() -> {
//        					if (client.options != null) {
//        						client.options.debugKey.setPressed(true);
//        						// Release after a short delay to simulate key press
//        						try {
//        							Thread.sleep(50);
//        						} catch (InterruptedException e) {
//        							// Ignore
//        						}
//        						client.options.debugKey.setPressed(false);
//        					}
//        				});
    				} else if (cmd.equals("swap")) {
    					// Swap main/off hand
    					client.execute(() -> {
    						if (client.player != null && client.interactionManager != null) {
    							client.options.swapHandsKey.setPressed(true);
    							try {
    								Thread.sleep(50);
    							} catch (InterruptedException e) {
    								// Ignore
    							}
    							client.options.swapHandsKey.setPressed(false);
    						}
    					});
    					System.out.println("Swapping main/off hand");
                	} 
//    				else if (cmd.equals("fov") {
//    					String num = extraMatcher.group(2);
//    					if (num != null) {
//    						int fovChange = Integer.parseInt(num);
//    						client.execute(() -> {
//    							if (client.options != null) {
//    								float newFov = fovChange;
//    								newFov = MathHelper.clamp(newFov, 30.0f, 110.0f);
//    								client.options.GameOptions.fov = newFov;
//    								System.out.println("Set FOV to: " + newFov);
//    							}
//    						});
//    					}
//                	}
                }
                
            	if (movementMatcher.find()) {
            		String cmd = movementMatcher.group(1).toLowerCase();
                    String num = movementMatcher.group(2);
                    
                    switch (cmd) {
                        case "w", "forward" -> handleMove(MinecraftClient.getInstance(), num == null ? 0.25f : Float.parseFloat(num), 0);
                        case "s", "backward" -> handleMove(MinecraftClient.getInstance(), num == null ? 0.25f : Float.parseFloat(num), 1);
                        case "a", "left" -> handleMove(MinecraftClient.getInstance(), num == null ? 0.25f : Float.parseFloat(num), 2);
                        case "d", "right" -> handleMove(MinecraftClient.getInstance(), num == null ? 0.25f : Float.parseFloat(num), 3);
                        case "sprint" -> {
                            
                            client.execute(() -> {
                                sprintToggle = !sprintToggle;
                                if (!sprintToggle) {
                                    if (client.options != null) client.options.sprintKey.setPressed(false);
                                    if (client.player != null) client.player.setSprinting(false);
                                }
                            });
                        }
                        case "jump" -> {
                            
                            client.execute(() -> {
                                if (client.player != null) {
                                    client.player.jump();
                                }
                            });
                        }
                        case "sneak" -> {
                            
                            client.execute(() -> {
                                sneakToggle = !sneakToggle;
                                if (!sneakToggle && client.options != null) {
                                    client.options.sneakKey.setPressed(false);
                                }
                            });
                        }
                    }
            	}
            	
            	if (swimMatcher.find()) {
            		String cmd = swimMatcher.group(1).toLowerCase();
    				
    				client.execute(() -> {
    					if (client.player != null) {
    					    var player = client.player;

    					    // Detect if we're actually in water/lava (flowing or still)
    					    boolean inWater = player.isTouchingWater() || player.isSubmergedIn(FluidTags.WATER);
    					    boolean inLava  = player.isInLava() || player.isSubmergedIn(FluidTags.LAVA);
    					    boolean inFluid = inWater || inLava;

    					    var jump  = client.options.jumpKey;
    					    var sneak = client.options.sneakKey;

    					    if ("swim".equals(cmd)) {
    					        if (inFluid) {
    					        	isSwimming = true;
    					            jump.setPressed(true);
    					            sneak.setPressed(false);
    					        } else {
    					        	isSwimming = false;
    					            jump.setPressed(false);
    					            sneak.setPressed(false);
    					        }
    					    } else if ("dive".equals(cmd)) {
    					        if (inFluid) {
    					        	isSwimming = true;
    					            sneak.setPressed(true);
    					            jump.setPressed(false);
    					        } else {
    					        	isSwimming = false;
    					            sneak.setPressed(false);
    					            jump.setPressed(false);
    					        }
    					    }
    					}
    				});
            	}
            	
            	if (resetMatcher.find()) {
    				// Reset all queued actions and states
    				queuedBreakBlocks = 0;
    				queuedPlaceBlocks = 0;
    				breakingPos = null;
    				breakingSide = null;
    				moveDistanceRemaining = 0.0;
    				moveDirection = -1;
    				prevPos = null;
    				moveStuckTicks = 0;
    				useTicksRemaining = 0;
    				// toggle states
    				sprintToggle = false;
    				sneakToggle = false;
    				
    				// Also release all movement keys immediately
    				
    				client.execute(() -> {
    					if (client.options != null) {
    						releaseMovementKeys(client.options);
    						client.options.sprintKey.setPressed(false);
    						client.options.sneakKey.setPressed(false);
    					}
    					// Stop sprinting as well
    					if (client.player != null) {
    						client.player.setSprinting(false);
    					}
    				});
    				
    				System.out.println("Reset all queued actions and states");
    			}
            	
            	if (lookMatcher.find()) {
            		String cmd = lookMatcher.group(1).toLowerCase();
    				String num = lookMatcher.group(2);
    				
    				switch (cmd) {
    					case "lu", "look up" -> handleLook(MinecraftClient.getInstance(), num == null ? 5f : Float.parseFloat(num), 0);
    					case "ld", "look down" -> handleLook(MinecraftClient.getInstance(), num == null ? 5f : Float.parseFloat(num), 1);
    					case "ll", "look left" -> handleLook(MinecraftClient.getInstance(), num == null ? 5f : Float.parseFloat(num), 2);
    					case "lr", "look right" -> handleLook(MinecraftClient.getInstance(), num == null ? 5f : Float.parseFloat(num), 3);
    				}
            	}
            	
            	if (clickMatcher.find()) {
            		String cmd = clickMatcher.group(1).toLowerCase();
    				String num = clickMatcher.group(2);
    				int count = num == null ? 1 : Math.max(1, Math.round(Float.parseFloat(num)));
    				switch (cmd) {
    					case "rc", "right click" -> handleClick(MinecraftClient.getInstance(), count, true);
    					case "lc", "left click" -> handleClick(MinecraftClient.getInstance(), count, false);
    					case "dc", "double click" -> handleClick(MinecraftClient.getInstance(), Math.max(2, count), true);
    				}
    			}
    			
    			if (screenMatcher.find()) {
    				String cmd = screenMatcher.group(1).toLowerCase();
    				
    				switch (cmd) {
    					case "inv", "inventory" -> {
    						
    						client.execute(() -> {
    							if (client.player != null && client.currentScreen == null) {
    								client.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player));
    							}
    						});
    					}
    					case "close", "exit" -> {
    						
    						client.execute(() -> {
    							if (client.currentScreen != null) {
    								client.setScreen(null);
    							}
    						});
    					}
    				}
            	}
    			
    			if (slotMatcher.find()) {
    				System.out.println("Slot matcher found! Groups: " + slotMatcher.group(1) + ", " + slotMatcher.group(2) + ", " + slotMatcher.group(3));
    				
    				String num = slotMatcher.group(2);
    				String cmd = slotMatcher.group(3);
    				
    				// Handle case where cmd might be null (just "slot X" without action)
    				if (cmd == null) {
    					cmd = "pickup"; // default action
    				} else {
    					cmd = cmd.toLowerCase();
    				}
    				
    				// Create final variable for lambda
    				final String finalCmd = cmd;
    				
    				System.out.println("Slot command: slot=" + num + ", action=" + finalCmd + ", screen=" + (client.currentScreen != null ? client.currentScreen.getClass().getSimpleName() : "null"));
    				
    				if (client.currentScreen == null) {
    					if (num != null) {
    						int slot = Integer.parseInt(num);
    						if (slot >= 1 && slot <= 9) {
    							System.out.println("Setting hotbar slot to: " + (slot - 1));
    							client.execute(() -> {
    								if (client.player != null) {
    									client.player.getInventory().setSelectedSlot(slot - 1);
    								}
    							});
    						}
    					}
    				}
    				
    				else {
    					// If in a screen, try to click the slot in the GUI
    					if (num != null) {
    						int slot = Integer.parseInt(num);
    						int slotIndex = slot - 1;
    						
    						System.out.println("Attempting slot interaction: slot=" + slotIndex + ", action=" + finalCmd);
    						
    						client.execute(() -> {
    							if (client.interactionManager != null && client.player != null && client.currentScreen instanceof HandledScreen<?> handledScreen) {
    								var screenHandler = handledScreen.getScreenHandler();
    								
    								System.out.println("Screen handler has " + screenHandler.slots.size() + " slots");
    								
    								// Validate slot index
    								if (slotIndex >= 0 && slotIndex < screenHandler.slots.size()) {
    									// Special handling: emulate vanilla double-click collect behavior
    									if ("all".equals(finalCmd)) {
    										doubleClickCollect(client, handledScreen, slotIndex);
    											return;
    									}
    									
    									net.minecraft.screen.slot.SlotActionType actionType;
    									int button = 0;
    									
    									switch (finalCmd) {
    										case "move" -> {
    											actionType = net.minecraft.screen.slot.SlotActionType.QUICK_MOVE;
    											button = 0;
    										}
    										case "craft" -> {
    											actionType = net.minecraft.screen.slot.SlotActionType.QUICK_CRAFT;
    											button = 0;
    										}
    										case "throw" -> {
    											actionType = net.minecraft.screen.slot.SlotActionType.THROW;
    											button = 0; // throw one item
    										}
    										default -> {
    											actionType = net.minecraft.screen.slot.SlotActionType.PICKUP;
    											button = 0; // left click
    										}
    									}
    									
    									System.out.println("Executing slot click: slot=" + slotIndex + ", button=" + button + ", action=" + actionType);
    									
    									// Use interaction manager to properly sync with server
    									client.interactionManager.clickSlot(
    										screenHandler.syncId,
    										slotIndex,
    										button,
    										actionType,
    										client.player
    									);
    								} else {
    									System.out.println("Invalid slot index: " + slotIndex + " (max: " + (screenHandler.slots.size() - 1) + ")");
    								}
    							} else {
    								System.out.println("Cannot interact with slot - missing requirements");
    							}
    						});
    					}
    				}
    			}
    			
    			if (lockonMatcher.find()) {
                    
                    client.execute(() -> {
                        String state = lockonMatcher.group(2).toLowerCase();
                        if (state.equals("on")) {
                            LockOnManager.setEnabled(true);
                        } else if (state.equals("off")) {
                            LockOnManager.setEnabled(false);
                        }
                        String entityType = lockonMatcher.group(3).toLowerCase();
                        if (!entityType.isEmpty()) {
                            LockOnManager.setEntityTypeKey(entityType);
                        }
                        // When enabling lock-on, auto-pick the nearest target so camera starts tracking immediately
                        if (LockOnManager.isEnabled()) {
                            LockOnManager.setTargetingCommand("nearest");
                        }
                    });
                }
    			
    			if (targetMatcher.find()) {
                    
                    client.execute(() -> {
                        String command = targetMatcher.group(2).toLowerCase();
                        if (LockOnManager.isEnabled()) {
                            LockOnManager.setTargetingCommand(command);
                            System.out.println("Targeting command: " + command);
                        } else {
                            System.out.println("Lock-on is not enabled. Use 'lock on [entity_type]' first.");
                        }
                    });
                }
    			
    			if (toggleMatcher.find()) {
				
				client.execute(() -> {
					String action = toggleMatcher.group(2).toLowerCase();
					switch (action) {
						case "attack", "break" -> {
							// Toggle breaking mode: if queued, stop; if not, start queuing indefinitely
							if (queuedBreakBlocks > 0) {
								queuedBreakBlocks = 0;
							} else {
								queuedBreakBlocks = Integer.MAX_VALUE; // effectively infinite until stopped
							}
						}
						case "place" -> {
							// Toggle placing mode similarly
							if (queuedPlaceBlocks > 0) {
								queuedPlaceBlocks = 0;
							} else {
								queuedPlaceBlocks = Integer.MAX_VALUE;
							}
						}
					}
				});
			}
    			
    			// Handle recipe book search first
                if (rbSearchMatcher.find()) {
                    
                    String query = rbSearchMatcher.group(1).trim();
                    client.execute(() -> setRecipeBookSearch(client, query));
                }

                // Handle crafting commands: prefer index, then by-name, then quick
                if (craftIndexMatcher.find()) {
                    
                    int idx = Integer.parseInt(craftIndexMatcher.group(1));
                    boolean all = craftIndexMatcher.group(2) != null && craftIndexMatcher.group(2).equalsIgnoreCase("all") || (craftIndexMatcher.group(2) != null && craftIndexMatcher.group(2).equalsIgnoreCase("max"));
                    client.execute(() -> handleCraftByIndex(client, idx, all));
                } 
            });
    	});
        

        String category = "category." + ChatPlaysMcMod.MOD_ID + ".controls";

        // Drive queued interactions each tick on the client thread
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        // Overlay slot indices using screen events
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen<?>) {
                ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, delta) -> {
                    renderSlotIndices((HandledScreen<?>) s, drawContext);
                });
            }
            
            // Render recipe indices for any screen with a RecipeBook (Inventory, Crafting, Furnace)
            if (screen instanceof InventoryScreen || screen instanceof CraftingScreen || screen instanceof FurnaceScreen) {
                ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, delta) -> {
                    renderRecipeBookButtonIndices(s, drawContext);
                });
            }
        });
        
        // Register HUD rendering callback
        HudRenderCallback.EVENT.register((DrawContext drawContext, RenderTickCounter tickCounter) -> {
            // true = ignore freeze (typical for HUD), false = respect freeze
            float tickDelta = tickCounter.getTickProgress(true);
            renderChatPlaysHUD(drawContext, tickDelta);
        });
    }

    private void onClientTick(MinecraftClient client) {
    	if (isSwimming) {
    		var player = client.player;
    		
    		// Check if player is actually touching water or lava using proper fluid detection
    		boolean inWater = player.isTouchingWater() || player.isSubmergedIn(FluidTags.WATER);
    		boolean inLava = player.isInLava() || player.isSubmergedIn(FluidTags.LAVA);
    		boolean inFluid = inWater || inLava;
    		
    		if (inFluid) {
				// Player is in fluid - reset the exit buffer and keep swimming
				swimExitBuffer = 0;
				// The jump/sneak keys are already set from the initial swim/dive command
			} else {
				// Player is not in fluid - start counting exit buffer
				swimExitBuffer++;
				// Only stop swimming after being out of water for 10 ticks (0.5 seconds)
				if (swimExitBuffer >= 10) {
					isSwimming = false;
					client.options.jumpKey.setPressed(false);
					client.options.sneakKey.setPressed(false);
					swimExitBuffer = 0;
				}
				// Otherwise keep swimming - this handles brief bobbing out of water
			}
    	}
    	
        if (client.player == null || client.interactionManager == null || client.world == null) return;

        // Apply sprint/sneak toggles every tick
        if (client.options != null) {
            client.options.sprintKey.setPressed(sprintToggle);
            client.options.sneakKey.setPressed(sneakToggle);
        }

//        // Track camera toward current lock-on target (if any)
//        if (LockOnManager.isEnabled()) {
//            trackCameraToTarget(client, 30.0f, 30.0f);
//        }

        // Process breaking first to feel responsive
        if (queuedBreakBlocks > 0) {
            if (client.crosshairTarget instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = bhr.getBlockPos();
                Direction side = bhr.getSide();

                // Start or continue breaking
                if (breakingPos == null || !breakingPos.equals(pos) || breakingSide != side) {
                    // Start new break on this block
                    if (client.interactionManager.attackBlock(pos, side)) {
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                    breakingPos = pos;
                    breakingSide = side;
                } else {
                    // Continue progress on the same block
                    if (client.interactionManager.updateBlockBreakingProgress(pos, side)) {
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                }

                // If block became air, count it and reset state
                if (client.world.getBlockState(pos).isAir()) {
                    queuedBreakBlocks--;
                    breakingPos = null;
                    breakingSide = null;
                    brokenBlockIndex++; // for buildingMine
                }
            } else if (client.crosshairTarget instanceof EntityHitResult ehr) {
                // Targeting an entity, attack it once per queuedBreakBlocks unit
            	Entity target = ehr.getEntity();
                if (target != null) {
                    client.interactionManager.attackEntity(client.player, target);
                    client.player.swingHand(Hand.MAIN_HAND);
                    queuedBreakBlocks--;
                }
            } else {
                // Not targeting a block, cancel any current progress
                if (breakingPos != null) {
                    client.interactionManager.cancelBlockBreaking();
                    breakingPos = null;
                    breakingSide = null;
                }
            }
        } else {
            // No break queued; ensure we aren't stuck in breaking state
            if (breakingPos != null) {
                client.interactionManager.cancelBlockBreaking();
                breakingPos = null;
                breakingSide = null;
            }
        }
        
        // Drive holding of the Use key while an item is being used
        if (useTicksRemaining > 0) {
            client.options.useKey.setPressed(true);
            useTicksRemaining--;
        } else {
            client.options.useKey.setPressed(false);
        }
        
        if (queuedBreakBlocks > 0 && buildingShaft) {
        	// While building a shaft, look straight down and break blocks below
        	if (client.player != null) {
				// Break 3 blocks, look down 26 degrees, break 4 blocks, look to 65 degrees, break block, look at 0 degrees, move forward slightly, repeat
				int cycleIndex = brokenBlockIndex % 8;
				if (cycleIndex < 3) {
					// First 3 blocks: look down 26 degrees
					client.player.setPitch(0.0f);
				} else if (cycleIndex < 7) {
					// Next 4 blocks: look down 65 degrees
					client.player.setPitch(26.0f);
				} else {
					// Last block: look level and move forward slightly
					client.player.setPitch(65.0f);
					Vec3d look = client.player.getRotationVec(1.0f).normalize().multiply(0.3); // move forward 0.3 blocks
					client.player.setVelocity(look.x, client.player.getVelocity().y, look.z);
				}
			}
        }
                
        if (queuedBreakBlocks > 0 && buildingMine) {
			// While mining, break block immediately in front, then look down 45 degrees, break next, move forward, repeat
			if (client.player != null) {
				if (brokenBlockIndex % 2 == 0) {
					// Look down 45 degrees
					client.player.setPitch(45.0f);
				} else {
					// Level pitch and move forward slightly
					client.player.setPitch(0.0f);
					Vec3d look = client.player.getRotationVec(1.0f).normalize().multiply(0.3); // move forward 0.3 blocks
					client.player.setVelocity(look.x, client.player.getVelocity().y, look.z);
				}
			}
		} else if (buildingMine) {
			// Finished mining
			buildingMine = false;
		}

        if (queuedPlaceBlocks > 0 && buildingTower) {
			// While building a tower, keep looking down and moving up
			if (client.player != null) {
				client.player.setPitch(90.0f); // look straight down
				client.player.setVelocity(client.player.getVelocity().x, 0.3, client.player.getVelocity().z); // move up
			}
		} else if (buildingTower) {
			// Finished building tower
			buildingTower = false;
			if (client.player != null) {
				client.player.setVelocity(client.player.getVelocity().x, 0.0, client.player.getVelocity().z); // stop vertical movement
			}
		} else if (queuedPlaceBlocks > 0 && buildingBridge) {
			// While building a bridge, keep moving forward slowly
			handleMove(client, 0.2f, 0); // move forward slowly
		} else if (buildingBridge) {
			// Finished building bridge
			buildingBridge = false;
			// Stop movement
			releaseMovementKeys(client.options);
			moveDirection = -1;
			moveDistanceRemaining = 0;
			prevPos = null;
			moveStuckTicks = 0;
		} 
        
        if (queuedPlaceBlocks > 0 && useTicksRemaining == 0) {
            if (attemptPlaceOrUseOnce(client)) {
                queuedPlaceBlocks--;
            } 
        }
	
        // Handle queued movement with real input so AutoJump works
        if (moveDirection != -1 && moveDistanceRemaining > 0) {
            GameOptions opts = client.options;
            switch (moveDirection) {
                case 0 -> opts.forwardKey.setPressed(true);
                case 1 -> opts.backKey.setPressed(true);
                case 2 -> opts.leftKey.setPressed(true);
                case 3 -> opts.rightKey.setPressed(true);
            }
            // Measure horizontal distance travelled this tick
            Vec3d cur = client.player.getPos();
            if (prevPos == null) prevPos = cur;
            double dx = cur.x - prevPos.x;
            double dz = cur.z - prevPos.z;
            double d = Math.hypot(dx, dz);
            if (d < 1.0e-4) {
                moveStuckTicks++;
            } else {
                moveStuckTicks = 0;
            }
            moveDistanceRemaining -= d;
            prevPos = cur;

            // Stop if done or stuck for ~0.5s (10 ticks)
            if (moveDistanceRemaining <= 0 || moveStuckTicks > 10) {
                releaseMovementKeys(opts);
                moveDirection = -1;
                moveDistanceRemaining = 0;
                prevPos = null;
                moveStuckTicks = 0;
            }
        } else {
            // Ensure keys are released if not moving
            releaseMovementKeys(client.options);
            moveDirection = -1;
            moveDistanceRemaining = 0;
            prevPos = null;
            moveStuckTicks = 0;
        }
    }

    private void releaseMovementKeys(GameOptions opts) {
        opts.forwardKey.setPressed(false);
        opts.backKey.setPressed(false);
        opts.leftKey.setPressed(false);
        opts.rightKey.setPressed(false);
    }

    private void handleClick(MinecraftClient client, int countBlocks, boolean rightClick) {
        if (client.player == null || client.interactionManager == null) return;

        if (rightClick) {
            ItemStack stack = client.player.getMainHandStack();
            UseAction action = stack.getUseAction();
            
            System.out.println("Right-click action: " + action + " with item " + stack.getItem().toString() + " x" + stack.getCount());

            // Handle consumables first: eat/drink repeatedly
            if (action == UseAction.EAT || action == UseAction.DRINK) {
                int uses = Math.max(1, countBlocks);
                for (int i = 0; i < uses; i++) {
                    ActionResult res = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    if (res.isAccepted()) {
                        client.player.swingHand(Hand.MAIN_HAND);
                        // Hold use for the item max use time to complete consumption
                        useTicksRemaining += Math.max(5, stack.getMaxUseTime(client.player));
                    } else {
                        break;
                    }
                }
                return;
            }

            // Bows/Crossbows/Tridents require holding use; start using and hold for max time
            if (action == UseAction.BOW || action == UseAction.CROSSBOW || action == UseAction.SPEAR) {
                // Initiate use if not already
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                useTicksRemaining = Math.max(useTicksRemaining, Math.max(10, stack.getMaxUseTime(client.player)));
                System.out.println("Holding use for " + useTicksRemaining + " ticks for action " + action);	
                return;
            }

            // Shields (blocking) – hold for N seconds (countBlocks interpreted as seconds)
            if (action == UseAction.BLOCK) {
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                int seconds = Math.max(1, countBlocks);
                useTicksRemaining = Math.max(useTicksRemaining, seconds * 20);
                return;
            }

            // Default: queue placements (previous behavior)
            queuedPlaceBlocks += Math.max(0, countBlocks);
        } else {
            // Left-click: queue attacks/breaks
            queuedBreakBlocks += Math.max(0, countBlocks);
        }
    }

    private boolean attemptPlaceOrUseOnce(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return false;
        // Try placing on the targeted block face first
        if (client.crosshairTarget instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            ActionResult res = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, bhr);
            if (res.isAccepted()) {
                client.player.swingHand(Hand.MAIN_HAND);
                return true;
            }
        }
        // If not looking at a block, try a short raycast to find one in reach
        HitResult hr = client.player.raycast(5.0D, 0.0F, false);
        if (hr instanceof BlockHitResult cast && cast.getType() == HitResult.Type.BLOCK) {
            ActionResult res = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, cast);
            if (res.isAccepted()) {
                client.player.swingHand(Hand.MAIN_HAND);
                return true;
            }
        }
        // Fallback to using the item in hand (may place if possible)
        ActionResult res = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        if (res.isAccepted()) {
            client.player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private void handleMove(MinecraftClient client, float distance, int direction) {
        if (client.player == null) return;
        moveDirection = direction;
        moveDistanceRemaining = Math.max(0, distance);
        prevPos = client.player.getPos();
        moveStuckTicks = 0;
    }
    
    private void handleLook(MinecraftClient client, float step, int direction) {
		if (client.player == null) return;

		float yaw = client.player.getYaw();
		float pitch = client.player.getPitch();

		if (direction == 0) { // up
			pitch = MathHelper.clamp(pitch - step, -90.0f, 90.0f);
		} else if (direction == 1) { // down
			pitch = MathHelper.clamp(pitch + step, -90.0f, 90.0f);
		} else if (direction == 2) { // left
			yaw -= step;
		} else if (direction == 3) { // right
			yaw += step;
		}

		client.player.setYaw(yaw);
		client.player.setPitch(pitch);
	}

    private void renderSlotIndices(HandledScreen<?> handled, DrawContext context) {
        var handler = handled.getScreenHandler();
        var tr = MinecraftClient.getInstance().textRenderer;
        int color = 0x44FFFF00; // semi-transparent yellow (ARGB)
        
        // Use accessor mixin to get the dynamic screen origin (works with recipe book etc.)
        int screenX = ((HandledScreenAccessor) handled).getX();
        int screenY = ((HandledScreenAccessor) handled).getY();
        
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            int x = screenX + slot.x + 2;
            int y = screenY + slot.y + 2;
            String text = Integer.toString(i + 1); // show 1-based index
            context.drawText(tr, text, x, y, color, true);
        }
        
    }
    
    private void renderRecipeBookButtonIndices(Object screen, DrawContext context) {
        RecipeBookWidget widget = getRecipeBookWidget(screen);
        if (widget == null) return;
        var recipeArea = ((RecipeBookWidgetAccessor)(Object) widget).getRecipesArea();
        if (recipeArea == null) return;
        var buttons = ((RecipeBookResultsAccessor)(Object) recipeArea).getResultButtons();
        if (buttons == null) return;
        for (int i = 0; i < buttons.size(); i++) {
            var btn = buttons.get(i);
            int x = btn.getX();
            int y = btn.getY();
            String text = Integer.toString(i + 1);
            context.drawText(MinecraftClient.getInstance().textRenderer, text, x + 2, y + 2, 0x44FF0000, true);
        }
    }

    private net.minecraft.client.gui.screen.recipebook.RecipeBookWidget getRecipeBookWidget(Object screen) {
        if (screen instanceof InventoryScreen inv) {
            return ((com.voltaicgrid.chatplaysmc.mixin.RecipeScreenAccessor)(Object) inv).getRecipeBook();
        }
        if (screen instanceof CraftingScreen cs) {
            return ((com.voltaicgrid.chatplaysmc.mixin.RecipeScreenAccessor)(Object) cs).getRecipeBook();
        }
        if (screen instanceof FurnaceScreen fs) {
            return ((com.voltaicgrid.chatplaysmc.mixin.RecipeScreenAccessor)(Object) fs).getRecipeBook();
        }
        return null;
    }

    private void setRecipeBookSearch(MinecraftClient client, String query) {
        Object screen = client.currentScreen;
        RecipeBookWidget widget = getRecipeBookWidget(screen);
        if (widget == null) {
            System.out.println("rb search ignored: no recipe book on current screen");
            return;
        }
        var search = ((RecipeBookWidgetAccessor)(Object) widget).getSearchField();
        if (search != null) {
            search.setText(query);
            // TextFieldWidget has a changed listener; setText triggers refresh.
        }
    }

    private void handleCraftByIndex(MinecraftClient client, int index1Based, boolean craftAll) {
        if (client.interactionManager == null || client.player == null) return;
        Screen screen = client.currentScreen;
        int syncId = -1;
        if (screen instanceof InventoryScreen || screen instanceof CraftingScreen || screen instanceof FurnaceScreen) {
        	syncId = ((net.minecraft.client.gui.screen.ingame.RecipeBookScreen)(Object) screen).getScreenHandler().syncId;
        } else {
			System.out.println("craft index ignored: not in a valid screen (inv/crafting/furnace)");
			return;
		}
        RecipeBookWidget widget = getRecipeBookWidget(screen);
        if (widget == null) {
            System.out.println("craft index ignored: no recipe book on current screen");
            return;
        }
        var area = ((RecipeBookWidgetAccessor)(Object) widget).getRecipesArea();
        if (area == null) return;
        var buttons = ((RecipeBookResultsAccessor)(Object) area).getResultButtons();
        if (buttons == null || buttons.isEmpty()) return;
        int idx = index1Based - 1;
        if (idx < 0 || idx >= buttons.size()) return;
        var btn = buttons.get(idx);
        var recipeId = btn.getCurrentId();
        
        System.out.println("Crafting recipe index " + index1Based + " (all=" + craftAll + "): " + (recipeId != null ? recipeId : "-"));
        
        client.interactionManager.clickRecipe(
    		syncId,
			recipeId,
			true
		);
    }
    
    private void doubleClickCollect(MinecraftClient client, HandledScreen<?> handledScreen, int slotIndex) {
        var screenHandler = handledScreen.getScreenHandler();
        if (screenHandler == null || client.player == null || client.interactionManager == null) return;
        if (slotIndex < 0 || slotIndex >= screenHandler.slots.size()) return;
        var slot = screenHandler.slots.get(slotIndex);
        var stackInSlot = slot.getStack();
        if (stackInSlot == null || stackInSlot.isEmpty()) {
            System.out.println("doubleClickCollect: target slot is empty");
            return;
        }
        var cursor = screenHandler.getCursorStack();
        boolean cursorMatches = canStackTogether(cursor, stackInSlot);
        // Step 1: if cursor doesn't match, pick up from the clicked slot to load cursor with target item
        if (!cursorMatches) {
            client.interactionManager.clickSlot(screenHandler.syncId, slotIndex, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, client.player);
        }
        // Step 2: perform PICKUP_ALL to collect matching stacks across inventory
        client.interactionManager.clickSlot(screenHandler.syncId, slotIndex, 0, net.minecraft.screen.slot.SlotActionType.PICKUP_ALL, client.player);
    }

    private boolean canStackTogether(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return false;
        if (!a.isStackable() || !b.isStackable()) return false;
        if (a.getItem() != b.getItem()) return false;
        // For most items, matching the item type is sufficient; server will reject incompatible components.
        return true;
    }
    
    private void renderChatPlaysHUD(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        
        var textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int y = 10; // Start from top of screen
        int lineHeight = 12;
        
        // Background for better readability
        int hudWidth = 200;
        int hudHeight = 120;
        context.fill(10, 5, 10 + hudWidth, 5 + hudHeight, 0x80000000); // Semi-transparent black background
        
        // Player coordinates
        Vec3d pos = client.player.getPos();
        String coordText = String.format("XYZ: %.1f, %.1f, %.1f", pos.x, pos.y, pos.z);
        context.drawText(textRenderer, coordText, 15, y, 0xFFFFFFFF, true);
        y += lineHeight;
        
        // Player rotation
        String rotText = String.format("Yaw: %.1f° Pitch: %.1f°", client.player.getYaw(), client.player.getPitch());
        context.drawText(textRenderer, rotText, 15, y, 0xFFFFFFFF, true);
        y += lineHeight;
        
        // Current action queues
        if (queuedBreakBlocks > 0) {
            String breakText = queuedBreakBlocks == Integer.MAX_VALUE ? "Breaking: ∞" : "Breaking: " + queuedBreakBlocks;
            context.drawText(textRenderer, breakText, 15, y, 0xFFFF5555, true);
            y += lineHeight;
        }
        
        if (queuedPlaceBlocks > 0) {
            String placeText = queuedPlaceBlocks == Integer.MAX_VALUE ? "Placing: ∞" : "Placing: " + queuedPlaceBlocks;
            context.drawText(textRenderer, placeText, 15, y, 0xFF55FF55, true);
            y += lineHeight;
        }
        
        if (useTicksRemaining > 0) {
            String useText = "Using: " + (useTicksRemaining / 20.0f) + "s";
            context.drawText(textRenderer, useText, 15, y, 0xFF5555FF, true);
            y += lineHeight;
        }
        
        // Movement status
        if (moveDirection != -1 && moveDistanceRemaining > 0) {
            String[] directions = {"Forward", "Backward", "Left", "Right"};
            String moveText = directions[moveDirection] + ": " + String.format("%.2f", moveDistanceRemaining);
            context.drawText(textRenderer, moveText, 15, y, 0xFFFFFF55, true);
            y += lineHeight;
        }
        
        if (isSwimming) {
			context.drawText(textRenderer, "Swimming", 15, y, 0xFF55FFFF, true);
			y += lineHeight;
		}
        
        // Building status
        if (buildingTower) {
        	context.drawText(textRenderer, "Building Tower", 15, y, 0xFFFF8855, true);
            y += lineHeight;
        } else if (buildingBridge) {
        	context.drawText(textRenderer, "Building Bridge", 15, y, 0xFFFF8855, true);
            y += lineHeight;
        } else if (buildingMine) {
			context.drawText(textRenderer, "Building Mine", 15, y, 0xFFFF8855, true);
			y += lineHeight;
		}
        
        // Toggle states
        String toggles = "";
        if (sprintToggle) toggles += "Sprint ";
        if (sneakToggle) toggles += "Sneak ";
        if (LockOnManager.isEnabled()) toggles += "Lock ";
        if (!toggles.isEmpty()) {
        	context.drawText(textRenderer, "Active: " + toggles.trim(), 15, y, 0xFF55FFFF, true);
            y += lineHeight;
        }
        
        // Held item info
        ItemStack mainHand = client.player.getMainHandStack();
        if (!mainHand.isEmpty()) {
            String itemText = mainHand.getItem().getName().getString() + " x" + mainHand.getCount();
            
            // Add durability as percentage if item is damageable
            if (mainHand.getMaxDamage() > 0) {
                int currentDurability = mainHand.getMaxDamage() - mainHand.getDamage();
                double durabilityPercent = (double) currentDurability / mainHand.getMaxDamage() * 100.0;
                itemText += String.format(" (%.1f%%)", durabilityPercent);
            }
            
            context.drawText(textRenderer, itemText, 15, y, 0xFFAAAAAA, true);
            
            // Check if this is the food satchel using the registry identifier
            var itemId = Registries.ITEM.getId(mainHand.getItem());
            if (itemId.toString().equals("chat_plays_mc:foot_satchel")) { 
        		ContainerComponent container = mainHand.get(DataComponentTypes.CONTAINER);
        		if (container != null) {
					context.drawText(textRenderer, "Contains: " + container.stream().count() + " items", 15, y + lineHeight, 0xFFAAAAAA, true);
				}
            }
        }
    }
}
