/*
MIT License

Copyright (c) 2022 CodeBase Java Academy

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package net.voigon;

import com.google.common.reflect.ClassPath;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;

/**
 * Allows listener auto registration based on package name.
 * To use, instantiate the class and invoke one of the register method.
 * @author Voigon
 */
@RequiredArgsConstructor
public class ListenerAutoRegistration {

    /**
     * Your plugin instance
     */
    @NonNull
    final JavaPlugin
            plugin;

    /**
     * Should listeners marked with @DevServerListener be loaded
     */
    final boolean
            loadDevListeners;

    /**
     * Register all listeners in given package, including its sub packages
     * @param packageName given package name
     */
    public void register(String packageName) {
        register(packageName, true);

    }

    /**
     * Register all listeners in given package
     * @param packageName given package name
     * @param deep set to true to include sub packages, otherwise will include only classes from given package
     */
    @SneakyThrows
    public void register(String packageName, boolean deep) {
        ClassLoader classLoader = plugin.getClass().getClassLoader();
        ClassPath classPath = ClassPath.from(classLoader);
        for (ClassPath.ClassInfo classInfo : deep ? classPath.getTopLevelClassesRecursive(packageName) : classPath.getTopLevelClasses(packageName)) {
            try {
                Class<?> clazz = classLoader.loadClass(classInfo.getName());
                if (!Listener.class.isAssignableFrom(clazz))
                    continue;

                boolean devListener = clazz.isAnnotationPresent(DevServerListener.class);
                if (devListener && !loadDevListeners)
                    continue;

                Constructor<?> constructor = clazz.getConstructors()[0];
                Listener instance = (Listener) constructor.newInstance(constructor.getParameterCount() == 0 ? new Object[0] : new Object[] {plugin});
                Bukkit.getPluginManager().registerEvents(instance, plugin);

                plugin.getLogger().info("Loaded " + (devListener ? "dev" : "") + " listener " + clazz.getSimpleName() + "!");

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

    }

    /**
     * Marks listener as one that should only be loaded if loadDevListeners is set to true
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DevServerListener { }

}