package dev.haze.client;

import java.lang.reflect.Method;

public final class Compat {
    private static boolean looked;
    private static Object irisApi;
    private static Method shaderPackInUse;
    private static boolean dewLooked;
    private static Method dewyNight;

    private Compat() {
    }

    static boolean modLoaded(String id) {
        try {
            Class<?> fabric = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = fabric.getMethod("getInstance").invoke(null);
            return (Boolean) fabric.getMethod("isModLoaded", String.class).invoke(loader, id);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        for (String name : new String[]{"net.neoforged.fml.ModList", "net.minecraftforge.fml.ModList"}) {
            try {
                Class<?> modList = Class.forName(name);
                Object list = modList.getMethod("get").invoke(null);
                if (list != null) {
                    return (Boolean) modList.getMethod("isLoaded", String.class).invoke(list, id);
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            }
        }
        return false;
    }

    static boolean dewyNight(long time) {
        if (!dewLooked) {
            dewLooked = true;
            try {
                dewyNight = Class.forName("dev.squall.client.Dew").getMethod("dewyNight", long.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                dewyNight = null;
            }
        }
        if (dewyNight == null) {
            return false;
        }
        try {
            return (Boolean) dewyNight.invoke(null, time);
        } catch (ReflectiveOperationException | RuntimeException e) {
            dewyNight = null;
            return false;
        }
    }

    public static boolean shaderPackInUse() {
        if (!looked) {
            looked = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                shaderPackInUse = api.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException | LinkageError e) {
                irisApi = null;
            }
        }
        if (irisApi == null) {
            return false;
        }
        try {
            return (Boolean) shaderPackInUse.invoke(irisApi);
        } catch (ReflectiveOperationException | RuntimeException e) {
            irisApi = null;
            return false;
        }
    }
}
