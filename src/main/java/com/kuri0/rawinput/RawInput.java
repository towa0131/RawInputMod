package com.kuri0.rawinput;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraft.command.ICommand;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import java.lang.reflect.Constructor;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;

@Mod(modid = RawInput.MODID, version = RawInput.VERSION)
public class RawInput
{
    public static final String MODID = "rawinput";
    public static final String VERSION = "1.1.1";
    
    public static Mouse mouse;
    public static Controller[] controllers;

    public static int dx = 0;
    public static int dy = 0;
    
    @SuppressWarnings("unchecked")
    private static ControllerEnvironment createDefaultEnvironment() throws ReflectiveOperationException {
        // Find constructor (class is package private, so we can't access it directly)
        Constructor<ControllerEnvironment> constructor = (Constructor<ControllerEnvironment>)
            Class.forName("net.java.games.input.DefaultControllerEnvironment").getDeclaredConstructors()[0];

        // Constructor is package private, so we have to deactivate access control checks
        constructor.setAccessible(true);
        // Create object with default constructor
        return constructor.newInstance();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        ClientCommandHandler.instance.registerCommand(new RescanCommand());
        Minecraft.getMinecraft().mouseHelper = new RawMouseHelper();
		controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

		Thread inputThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					int i = 0;
					while (i < controllers.length && mouse == null) {
						if (controllers[i].getType() == Controller.Type.MOUSE) {
							controllers[i].poll();
							float px = ((Mouse) controllers[i]).getX().getPollData();
							float py = ((Mouse) controllers[i]).getY().getPollData();
							float eps = 0.1f;
							if (Math.abs(px) > eps || Math.abs(py) > eps) {
								mouse = (Mouse) controllers[i];
								try {
									Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Found mouse"));
								} catch (Exception ignored) {}
							}
						}
						i++;
					}
					if (mouse != null) {
						mouse.poll();
						if (Minecraft.getMinecraft().currentScreen == null) {
							dx += mouse.getX().getPollData();
							dy += mouse.getY().getPollData();
						}
					}

					try {
						Thread.sleep(1L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
            }
        });
        
        inputThread.setName("inputThread");
        inputThread.start();
    }
}

