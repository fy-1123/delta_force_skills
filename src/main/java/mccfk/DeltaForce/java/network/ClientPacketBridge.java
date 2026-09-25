package mccfk.DeltaForce.java.network;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Lazily dispatches client-only warehouse callbacks from common packet code. */
final class ClientPacketBridge {
    private ClientPacketBridge() {
    }

    static void openWarehouse(Object snapshot) {
        try {
            Class<?> target = Class.forName("com.rzy.dealt_force_skills.client.ClientPacketHandlers", false,
                    ClientPacketBridge.class.getClassLoader());
            for (Method method : target.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) && method.getName().equals("openWarehouse")
                        && method.getParameterCount() == 1 && method.getParameterTypes()[0].isInstance(snapshot)) {
                    method.setAccessible(true);
                    method.invoke(null, snapshot);
                    return;
                }
            }
            throw new NoSuchMethodException("ClientPacketHandlers#openWarehouse");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to dispatch warehouse snapshot to client", exception);
        }
    }
}